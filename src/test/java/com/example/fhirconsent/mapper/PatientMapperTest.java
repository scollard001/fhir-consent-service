package com.example.fhirconsent.mapper;

import com.example.fhirconsent.dto.PatientRequest;
import com.example.fhirconsent.dto.PatientResponse;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PatientMapperTest {

    private final PatientMapper mapper = new PatientMapper();

    @Test
    void toFhir_mapsAllFieldsOntoPatientResource() {
        PatientRequest request = new PatientRequest(
                "Rivera",
                List.of("Sofia", "M"),
                LocalDate.of(1988, 4, 12),
                PatientRequest.AdministrativeGender.FEMALE,
                "MRN-100245"
        );

        Patient patient = mapper.toFhir(request);

        assertThat(patient.getNameFirstRep().getFamily()).isEqualTo("Rivera");
        assertThat(patient.getNameFirstRep().getGiven())
                .extracting(g -> g.getValueAsString())
                .containsExactly("Sofia", "M");
        assertThat(patient.getGender().toCode()).isEqualTo("female");
        assertThat(patient.getIdentifierFirstRep().getValue()).isEqualTo("MRN-100245");
    }

    @Test
    void toResponse_roundTripsThroughFhirResource() {
        Patient patient = mapper.toFhir(new PatientRequest(
                "Chen", List.of("Wei"), LocalDate.of(1975, 1, 1),
                PatientRequest.AdministrativeGender.MALE, "MRN-999"));
        patient.setId(new IdType("Patient", "abc123"));

        PatientResponse response = mapper.toResponse(patient);

        assertThat(response.fhirId()).isEqualTo("abc123");
        assertThat(response.familyName()).isEqualTo("Chen");
        assertThat(response.medicalRecordNumber()).isEqualTo("MRN-999");
        assertThat(response.gender()).isEqualTo("male");
    }
}
