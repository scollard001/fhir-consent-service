# ADR 0001: Use a HAPI FHIR *client* against an external FHIR store, not an embedded FHIR server

## Status
Accepted

## Context
HAPI FHIR offers two very different integration points:

1. **`hapi-fhir-jpaserver-starter`** - embed a full, spec-compliant FHIR server
   (with its own persistence, search indexing, validation, and REST API)
   directly inside this Spring Boot application.
2. **`hapi-fhir-client`** - use this application purely as a REST client
   against a FHIR server deployed and operated elsewhere.

This service's job is to encode *business logic* - "an insulin pump is
associated with a patient", "a partner may only read device data if an active
consent grants it" - on top of clinical data. It is not this team's job to
operate a general-purpose FHIR store, and coupling that operational burden
(schema migrations, search-parameter reindexing, FHIR version upgrades,
conformance testing) to every service that wants to read/write FHIR data does
not scale organizationally.

## Decision
This service depends only on `hapi-fhir-client` + `hapi-fhir-structures-r4`
and talks to a FHIR store over HTTP, configured via `fhir.server.base-url`.
Locally and in CI, that store is a disposable `hapiproject/hapi` JPA server
(see `docker-compose.yml` and the Testcontainers-based integration test).

## Consequences
- **Positive:** this service stays small, stateless, and independently
  deployable/scalable. Multiple business services (this one, a
  provider-facing UI backend, an analytics pipeline) can share one FHIR
  store without duplicating FHIR persistence logic.
- **Positive:** integration tests exercise a real FHIR server via
  Testcontainers, catching malformed resources and bad search parameters
  that a mocked client would hide (see ADR 0002 below, informally captured
  in the integration test's Javadoc).
- **Negative:** an extra network hop and an extra deployable (the FHIR
  server itself) compared to an embedded server.
- **Negative:** this service now depends on the FHIR store's uptime/latency
  characteristics; `fhir.client.socket-timeout-ms` and standard
  Spring retry/circuit-breaker patterns (not yet added here - see
  `docs/architecture.md` "Not Yet Implemented") should be layered on before
  this pattern is used for a production, latency-sensitive read path.

## Alternatives considered
- **Embedded `hapi-fhir-jpaserver-starter`:** rejected for the reasons above;
  reasonable if this were the *only* system of record for FHIR data and no
  other service needed direct FHIR access.
- **Direct JDBC/JPA against a custom schema, FHIR-shaped only at the API
  boundary:** rejected because it forfeits FHIR search semantics, validation,
  and the ecosystem of FHIR tooling (e.g. bulk export, SMART on FHIR) for
  marginal performance gains not currently needed at this scale.
