package com.example.fhirconsent.dto;

import java.time.LocalDate;

public record DeviceResponse(
        String fhirId,
        String patientFhirId,
        String manufacturer,
        String modelNumber,
        String serialNumber,
        String softwareVersion,
        String lotNumber,
        LocalDate manufactureDate,
        String udiCarrierHrf,
        String status
) {
}
