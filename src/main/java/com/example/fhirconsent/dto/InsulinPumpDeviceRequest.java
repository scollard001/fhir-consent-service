package com.example.fhirconsent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

/**
 * Request to associate an insulin pump device with a patient, using the
 * Unique Device Identifier (UDI) fields FDA/GUDID expect for implantable and
 * durable medical equipment.
 */
public record InsulinPumpDeviceRequest(
        @NotBlank String patientFhirId,
        @NotBlank String manufacturer,
        @NotBlank String modelNumber,
        @NotBlank String serialNumber,
        String softwareVersion,
        String lotNumber,
        @PastOrPresent LocalDate manufactureDate,
        @NotNull @NotBlank String udiCarrierHrf
) {
}
