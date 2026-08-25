# Architecture

## Component overview

```mermaid
flowchart LR
    subgraph Clients
        Clinician[Clinician-facing app]
        Partner[External partner: e.g. remote monitoring vendor]
    end

    subgraph fhir-consent-service
        API[REST API<br/>Patient / Device / Consent controllers]
        Gate[ExternalDataSharingController<br/>consent-gated read path]
        Svc[Service layer<br/>PatientService / DeviceService / ConsentService]
        Map[Mappers<br/>DTO &lt;-&gt; FHIR R4 model]
    end

    subgraph FHIR Store
        HAPI[HAPI FHIR JPA Server]
        PG[(PostgreSQL)]
    end

    Clinician -->|create Patient / Device / Consent| API
    Partner -->|read device data| Gate
    API --> Svc
    Gate --> Svc
    Svc --> Map
    Svc -->|HAPI FHIR client, REST| HAPI
    HAPI --> PG
```

## Resource model

| FHIR Resource | Represents | Key fields used |
|---|---|---|
| `Patient` | The person receiving care | `identifier` (MRN), `name`, `birthDate`, `gender` |
| `Device` | The insulin pump | `type` (SNOMED CT `469756000`), `patient` reference, `udiCarrier`, `manufacturer`/`modelNumber`/`serialNumber`/`lotNumber`, `version` |
| `Consent` | The patient's grant of data access to an external partner | `patient` reference, `status`, `scope`=`patient-privacy`, `category`=`IDSCL`, `provision.type`=`permit`, `provision.period`, `provision.actor` (partner, role `IRCP`), `provision.purpose` (v3 ActReason), `provision.class` (project-specific data categories) |

Device is intentionally modeled as a *generic* FHIR `Device` with a SNOMED CT
type coding rather than a custom resource/profile. That keeps it queryable by
any other system using standard `Device` search parameters, at the cost of
not having pump-specific fields (e.g. reservoir volume, basal rate schedule)
be strongly typed. A production system would likely publish a FHIR
`StructureDefinition` profile constraining `Device` for insulin pumps
specifically (see "Not Yet Implemented" below).

## Consent-gated data access flow

```mermaid
sequenceDiagram
    participant Partner as External Partner
    participant Gate as ExternalDataSharingController
    participant ConsentSvc as ConsentService
    participant DeviceSvc as DeviceService
    participant FHIR as HAPI FHIR Server

    Partner->>Gate: GET /partners/{partnerId}/patients/{patientId}/devices
    Gate->>ConsentSvc: hasActiveConsentForPartner(patientId, partnerId)
    ConsentSvc->>FHIR: search Consent?patient=...&status=active
    FHIR-->>ConsentSvc: Bundle of active Consents
    ConsentSvc-->>Gate: true/false (period covers now, actor matches)
    alt consent present
        Gate->>DeviceSvc: getDevicesForPatient(patientId)
        DeviceSvc->>FHIR: search Device?patient=...
        FHIR-->>DeviceSvc: Bundle of Devices
        DeviceSvc-->>Gate: DeviceResponse list
        Gate-->>Partner: 200 OK + devices
    else no consent
        Gate-->>Partner: 403 Forbidden (ProblemDetail)
    end
```

## Not yet implemented (explicitly out of scope for this example)

These are called out because a senior engineer reviewing this repo would
expect them to be named, not silently missing:

- **AuthN/AuthZ.** There is no OAuth2/SMART-on-FHIR bearer token validation
  on any endpoint. `partnerOrganizationId` in the URL is trusted as-is; in
  production this must come from a verified token (e.g. a SMART Backend
  Services client credentials grant scoped to that partner).
- **Audit logging.** Every consent check and data read should emit an
  `AuditEvent` FHIR resource (or equivalent) for compliance and breach
  investigation purposes.
- **Consent conflict/precedence rules.** This example assumes at most one
  relevant active Consent per (patient, partner) pair. Real consent
  management must handle overlapping, conflicting, or superseding consents.
- **Resilience.** No retry/circuit-breaker around the FHIR client calls.
- **Formal FHIR profiles.** Resources are shaped in code (see mappers) rather
  than validated against published `StructureDefinition`/`Consent` profiles.
