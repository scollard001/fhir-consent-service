package com.example.fhirconsent.mapper;

import com.example.fhirconsent.dto.ConsentRequest;
import com.example.fhirconsent.dto.ConsentResponse;
import com.example.fhirconsent.util.ConsentPurpose;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsentMapperTest {

    private final ConsentMapper mapper = new ConsentMapper();

    @Test
    void toFhir_grantsNamedPartnerAsRecipientForRequestedCategories() {
        ConsentRequest request = new ConsentRequest(
                "patient-42",
                "partner-glucovue",
                "GlucoVue Remote Monitoring Inc.",
                ConsentPurpose.TREATMENT,
                List.of("device-telemetry", "glucose-readings"),
                OffsetDateTime.parse("2026-01-01T00:00:00Z"),
                OffsetDateTime.parse("2027-01-01T00:00:00Z")
        );

        Consent consent = mapper.toFhir(request);

        assertThat(consent.getStatus().toCode()).isEqualTo("active");
        assertThat(consent.getPatient().getReference()).isEqualTo("Patient/patient-42");
        assertThat(consent.getProvision().getActorFirstRep().getReference().getIdentifier().getValue())
                .isEqualTo("partner-glucovue");
        assertThat(consent.getProvision().getClass_())
                .extracting(c -> c.getCode())
                .containsExactlyInAnyOrder("device-telemetry", "glucose-readings");
        assertThat(consent.getProvision().getPurposeFirstRep().getCode()).isEqualTo("TREAT");
    }

    @Test
    void toResponse_resolvesPurposeCodeBackToEnumName() {
        Consent consent = mapper.toFhir(new ConsentRequest(
                "patient-42", "partner-x", "Partner X", ConsentPurpose.RESEARCH,
                List.of("device-telemetry"),
                OffsetDateTime.parse("2026-01-01T00:00:00Z"), null));
        consent.setId(new IdType("Consent", "consent-1"));

        ConsentResponse response = mapper.toResponse(consent);

        assertThat(response.fhirId()).isEqualTo("consent-1");
        assertThat(response.purpose()).isEqualTo("RESEARCH");
        assertThat(response.partnerOrganizationId()).isEqualTo("partner-x");
        assertThat(response.effectiveTo()).isNull();
    }
}
