package com.example.fhirconsent.dto;

import com.example.fhirconsent.util.ConsentPurpose;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Request to record a patient's consent to share their data (including
 * connected-device telemetry) with a named external partner organization.
 */
public record ConsentRequest(
        @NotBlank String patientFhirId,
        @NotBlank String partnerOrganizationId,
        @NotBlank String partnerOrganizationName,
        @NotNull ConsentPurpose purpose,
        @NotEmpty List<@NotBlank String> dataCategories,
        @NotNull OffsetDateTime effectiveFrom,
        @FutureOrPresent OffsetDateTime effectiveTo
) {
}
