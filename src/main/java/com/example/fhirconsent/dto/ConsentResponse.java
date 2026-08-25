package com.example.fhirconsent.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ConsentResponse(
        String fhirId,
        String patientFhirId,
        String partnerOrganizationId,
        String partnerOrganizationName,
        String purpose,
        List<String> dataCategories,
        OffsetDateTime effectiveFrom,
        OffsetDateTime effectiveTo,
        String status
) {
}
