# fhir-consent-service

A reference Spring Boot service showing how to model **Patient**, **Device**,
and **Consent** on top of [HAPI FHIR](https://hapifhir.io/) (R4), using an
insulin pump as the example connected device: a patient is created, an
insulin pump is associated with them as a `Device`, and the patient's consent
to share that device's data with a named external partner is recorded,
enforced, and revocable as a FHIR `Consent` resource.

It's meant to be read, not just run: the code favors explicit FHIR modeling
decisions (with commentary on *why*) over abstraction for its own sake.

## Why this exists

Most HAPI FHIR examples show CRUD on a single resource in isolation. The
interesting engineering problem in real device-connected-health integrations
is usually the *relationships*: a device belongs to a patient, and a partner
should only ever see that device's data if a specific, scoped, time-bounded
consent says so. This repo's `ExternalDataSharingController` is the piece
that actually enforces that - see [`docs/architecture.md`](docs/architecture.md)
for the sequence diagram.

## Stack

- Java 17, Spring Boot 3.3
- Gradle (Kotlin DSL) build, wrapper included
- HAPI FHIR client (`hapi-fhir-client` + `hapi-fhir-structures-r4`) 7.4 - see
  [`docs/adr/0001-fhir-client-vs-embedded-server.md`](docs/adr/0001-fhir-client-vs-embedded-server.md)
  for why this is a *client* against an external FHIR store rather than an
  embedded FHIR server
- A [HAPI FHIR JPA server](https://hub.docker.com/r/hapiproject/hapi) as the
  actual FHIR store (run locally via Docker Compose, or via Testcontainers in tests)

## Project layout

```
src/main/java/com/example/fhirconsent/
├── config/       FHIR client + context wiring
├── controller/    REST endpoints (Patient, Device, Consent, external partner access)
├── service/       Business logic, talks to the FHIR store via IGenericClient
│   └── impl/
├── mapper/        DTO <-> FHIR R4 resource translation (pure functions, easy to unit test)
├── dto/           Request/response records - the API's actual public contract
├── exception/     Domain exceptions + RFC 7807 ProblemDetail translation
└── util/          Shared FHIR system/code constants
```

## Running it locally

```bash
# 1. Start a local HAPI FHIR JPA server + Postgres
docker compose up -d

# 2. Run the service (talks to http://localhost:8081/fhir by default)
./gradlew bootRun
```

### Example flow

```bash
# Create a patient
curl -s -X POST http://localhost:8080/api/v1/patients \
  -H 'Content-Type: application/json' \
  -d '{
        "familyName": "Alvarez",
        "givenNames": ["Diego"],
        "birthDate": "1990-03-15",
        "gender": "MALE",
        "medicalRecordNumber": "MRN-778001"
      }'
# => { "fhirId": "123", ... }

# Associate an insulin pump with that patient
curl -s -X POST http://localhost:8080/api/v1/patients/123/devices \
  -H 'Content-Type: application/json' \
  -d '{
        "patientFhirId": "123",
        "manufacturer": "Acme MedTech",
        "modelNumber": "AP-2000",
        "serialNumber": "SN-778812",
        "softwareVersion": "3.4.1",
        "lotNumber": "LOT-55",
        "manufactureDate": "2024-06-01",
        "udiCarrierHrf": "(01)00844588003288(11)240601(17)290601(10)LOT-55(21)SN-778812"
      }'

# Record the patient's consent to share device data with a partner
curl -s -X POST http://localhost:8080/api/v1/patients/123/consents \
  -H 'Content-Type: application/json' \
  -d '{
        "patientFhirId": "123",
        "partnerOrganizationId": "partner-glucovue",
        "partnerOrganizationName": "GlucoVue Remote Monitoring Inc.",
        "purpose": "TREATMENT",
        "dataCategories": ["device-telemetry", "glucose-readings"],
        "effectiveFrom": "2026-01-01T00:00:00Z",
        "effectiveTo": "2027-01-01T00:00:00Z"
      }'

# The partner can now read the patient's devices...
curl -s http://localhost:8080/api/v1/partners/partner-glucovue/patients/123/devices

# ...but an unrelated partner cannot (403 Forbidden, RFC 7807 body):
curl -s http://localhost:8080/api/v1/partners/some-other-partner/patients/123/devices
```

## Testing

```bash
./gradlew test --tests "com.example.fhirconsent.mapper.*"  # fast: pure mapper unit tests only, no Docker required
./gradlew build                                             # also runs the Testcontainers-based integration test, which
                                                              # starts a real hapiproject/hapi server and exercises the full
                                                              # patient -> device -> consent -> revoke lifecycle against it
```

The mapper layer (DTO <-> FHIR resource) is tested as plain unit tests. The
service layer is intentionally tested via one end-to-end integration test
against a real FHIR server rather than by mocking HAPI's fluent client API -
see the Javadoc on `PatientDeviceConsentIntegrationTest` for the reasoning.

> **Note:** this repo was authored in a sandboxed environment without network
> access, so `./gradlew build` has not been executed here, and
> `gradle/wrapper/gradle-wrapper.jar` is not included (see
> `gradle/wrapper/README-IMPORTANT.txt` for the one-time `gradle wrapper`
> command to generate it). The code is written against documented HAPI FHIR
> 7.4 / Spring Boot 3.3 / Gradle 8.10 APIs; run the build yourself before
> relying on it, and open an issue/PR for anything that doesn't compile as-is.

## Design notes worth reading

- [`docs/architecture.md`](docs/architecture.md) - component diagram, resource
  model table, consent-check sequence diagram, and an explicit "not yet
  implemented" list (auth, audit logging, resilience, formal FHIR profiles)
- [`docs/adr/0001-fhir-client-vs-embedded-server.md`](docs/adr/0001-fhir-client-vs-embedded-server.md)

## Compliance disclaimer

This is a **demonstration of FHIR modeling and API design patterns**, not a
compliance-ready system. It does not implement authentication/authorization,
audit logging, encryption-at-rest configuration, or a Business Associate
Agreement's worth of controls. Do not point it at real patient data.

## License

[MIT](LICENSE)
