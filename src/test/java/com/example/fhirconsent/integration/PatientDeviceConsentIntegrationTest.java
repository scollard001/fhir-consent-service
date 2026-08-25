package com.example.fhirconsent.integration;

import com.example.fhirconsent.dto.*;
import com.example.fhirconsent.service.ConsentService;
import com.example.fhirconsent.service.DeviceService;
import com.example.fhirconsent.service.PatientService;
import com.example.fhirconsent.util.ConsentPurpose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test exercising the full stack (controllers are not invoked
 * directly here, but the same Spring-managed services they delegate to are)
 * against a real, disposable HAPI FHIR JPA server started via Testcontainers.
 *
 * <p>We deliberately do NOT mock {@code IGenericClient}: HAPI's fluent client
 * API is a long method chain that terminates in {@code .execute()}, and a
 * mock of that chain mostly just re-asserts the mock setup rather than
 * proving the request we send is well-formed FHIR the server will accept.
 * A real server catches serialization/reference/search-parameter mistakes
 * that a mock cannot. See docs/adr/0001-fhir-client-vs-embedded-server.md.</p>
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class PatientDeviceConsentIntegrationTest {

    @Container
    static GenericContainer<?> fhirServer = new GenericContainer<>("hapiproject/hapi:v7.2.0")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/fhir/metadata").forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(3));

    @DynamicPropertySource
    static void fhirServerProperties(DynamicPropertyRegistry registry) {
        registry.add("fhir.server.base-url",
                () -> "http://" + fhirServer.getHost() + ":" + fhirServer.getMappedPort(8080) + "/fhir");
    }

    @Autowired
    private PatientService patientService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private ConsentService consentService;

    @Test
    void insulinPumpAndConsentLifecycle_endToEnd() {
        // 1. Create the patient.
        PatientResponse patient = patientService.createPatient(new PatientRequest(
                "Alvarez",
                List.of("Diego"),
                LocalDate.of(1990, 3, 15),
                PatientRequest.AdministrativeGender.MALE,
                "MRN-778001"
        ));
        assertThat(patient.fhirId()).isNotBlank();

        // 2. Associate an insulin pump device with the patient.
        DeviceResponse device = deviceService.registerInsulinPump(new InsulinPumpDeviceRequest(
                patient.fhirId(),
                "Acme MedTech",
                "AP-2000",
                "SN-778812",
                "3.4.1",
                "LOT-55",
                LocalDate.of(2024, 6, 1),
                "(01)00844588003288(11)240601(17)290601(10)LOT-55(21)SN-778812"
        ));
        assertThat(deviceService.getDevicesForPatient(patient.fhirId()))
                .extracting(DeviceResponse::fhirId)
                .contains(device.fhirId());

        // 3. Before any consent exists, a partner has no access.
        assertThat(consentService.hasActiveConsentForPartner(patient.fhirId(), "partner-glucovue")).isFalse();

        // 4. Patient consents to share device data with an external partner.
        ConsentResponse consent = consentService.recordDataSharingConsent(new ConsentRequest(
                patient.fhirId(),
                "partner-glucovue",
                "GlucoVue Remote Monitoring Inc.",
                ConsentPurpose.TREATMENT,
                List.of("device-telemetry", "glucose-readings"),
                OffsetDateTime.now().minusMinutes(1),
                OffsetDateTime.now().plusYears(1)
        ));
        assertThat(consent.status()).isEqualTo("active");

        // 5. The partner now passes the consent check.
        assertThat(consentService.hasActiveConsentForPartner(patient.fhirId(), "partner-glucovue")).isTrue();
        // An unrelated partner still does not.
        assertThat(consentService.hasActiveConsentForPartner(patient.fhirId(), "partner-unrelated")).isFalse();

        // 6. Patient revokes consent; access is withdrawn.
        consentService.revokeConsent(consent.fhirId());
        assertThat(consentService.hasActiveConsentForPartner(patient.fhirId(), "partner-glucovue")).isFalse();
        assertThat(consentService.getActiveConsentsForPatient(patient.fhirId())).isEmpty();
    }

    @BeforeAll
    static void logServerUrl() {
        System.out.println("HAPI FHIR test server: http://" + fhirServer.getHost()
                + ":" + fhirServer.getMappedPort(8080) + "/fhir");
    }
}
