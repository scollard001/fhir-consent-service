package com.example.fhirconsent.service;

import com.example.fhirconsent.dto.ConsentRequest;
import com.example.fhirconsent.dto.ConsentResponse;

import java.util.List;

public interface ConsentService {

    ConsentResponse recordDataSharingConsent(ConsentRequest request);

    /** Sets status to {@code inactive}. Does not delete the resource: the
     * historical grant remains part of the patient's consent audit trail. */
    ConsentResponse revokeConsent(String consentFhirId);

    List<ConsentResponse> getActiveConsentsForPatient(String patientFhirId);

    /**
     * Authorization check used by partner-facing data access endpoints: does
     * an active consent exist, for the given patient, naming this partner as
     * an authorized recipient, whose period covers "now"?
     */
    boolean hasActiveConsentForPartner(String patientFhirId, String partnerOrganizationId);
}
