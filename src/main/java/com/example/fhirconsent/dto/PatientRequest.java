package com.example.fhirconsent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.util.List;

/**
 * Inbound representation for creating a patient. Deliberately narrower than
 * the full FHIR Patient resource - this is the subset our intake process
 * actually collects; the mapper fills in the rest.
 */
public record PatientRequest(
        @NotBlank String familyName,
        @NotEmpty List<@NotBlank String> givenNames,
        @NotNull @Past LocalDate birthDate,
        @NotNull AdministrativeGender gender,
        @NotBlank String medicalRecordNumber
) {
    public enum AdministrativeGender {
        MALE, FEMALE, OTHER, UNKNOWN
    }
}
