package com.example.fhirconsent.controller;

import com.example.fhirconsent.dto.ConsentRequest;
import com.example.fhirconsent.dto.ConsentResponse;
import com.example.fhirconsent.service.ConsentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients/{patientFhirId}/consents")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsentResponse recordConsent(@PathVariable String patientFhirId,
                                          @Valid @RequestBody ConsentRequest request) {
        ConsentRequest scoped = new ConsentRequest(
                patientFhirId,
                request.partnerOrganizationId(),
                request.partnerOrganizationName(),
                request.purpose(),
                request.dataCategories(),
                request.effectiveFrom(),
                request.effectiveTo()
        );
        return consentService.recordDataSharingConsent(scoped);
    }

    @GetMapping
    public List<ConsentResponse> getActiveConsents(@PathVariable String patientFhirId) {
        return consentService.getActiveConsentsForPatient(patientFhirId);
    }

    @DeleteMapping("/{consentFhirId}")
    public ConsentResponse revokeConsent(@PathVariable String patientFhirId,
                                          @PathVariable String consentFhirId) {
        return consentService.revokeConsent(consentFhirId);
    }
}
