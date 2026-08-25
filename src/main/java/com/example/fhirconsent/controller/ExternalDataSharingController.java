package com.example.fhirconsent.controller;

import com.example.fhirconsent.dto.DeviceResponse;
import com.example.fhirconsent.exception.ConsentRequiredException;
import com.example.fhirconsent.service.ConsentService;
import com.example.fhirconsent.service.DeviceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Simulates the endpoint an external partner (e.g. a remote patient
 * monitoring vendor) would call to pull a patient's device data. Every read
 * is gated on an active {@code Consent} naming that partner as an authorized
 * recipient - this is the piece that actually makes patient consent
 * meaningful rather than just a resource sitting unused in the FHIR store.
 */
@RestController
@RequestMapping("/api/v1/partners/{partnerOrganizationId}/patients/{patientFhirId}")
public class ExternalDataSharingController {

    private final ConsentService consentService;
    private final DeviceService deviceService;

    public ExternalDataSharingController(ConsentService consentService, DeviceService deviceService) {
        this.consentService = consentService;
        this.deviceService = deviceService;
    }

    @GetMapping("/devices")
    public List<DeviceResponse> getDevicesIfConsented(@PathVariable String partnerOrganizationId,
                                                        @PathVariable String patientFhirId) {
        if (!consentService.hasActiveConsentForPartner(patientFhirId, partnerOrganizationId)) {
            throw new ConsentRequiredException(patientFhirId, partnerOrganizationId);
        }
        return deviceService.getDevicesForPatient(patientFhirId);
    }
}
