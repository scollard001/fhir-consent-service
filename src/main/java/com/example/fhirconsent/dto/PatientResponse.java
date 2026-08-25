package com.example.fhirconsent.dto;

import java.time.LocalDate;
import java.util.List;

public record PatientResponse(
        String fhirId,
        String familyName,
        List<String> givenNames,
        LocalDate birthDate,
        String gender,
        String medicalRecordNumber
) {
}
