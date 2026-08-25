package com.example.fhirconsent.service.impl;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.example.fhirconsent.dto.ConsentRequest;
import com.example.fhirconsent.dto.ConsentResponse;
import com.example.fhirconsent.exception.FhirResourceNotFoundException;
import com.example.fhirconsent.mapper.ConsentMapper;
import com.example.fhirconsent.service.ConsentService;
import com.example.fhirconsent.util.FhirSystems;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.Consent.ConsentState;
import org.hl7.fhir.r4.model.MethodOutcome;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class ConsentServiceImpl implements ConsentService {

    private final IGenericClient fhirClient;
    private final ConsentMapper consentMapper;

    public ConsentServiceImpl(IGenericClient fhirClient, ConsentMapper consentMapper) {
        this.fhirClient = fhirClient;
        this.consentMapper = consentMapper;
    }

    @Override
    public ConsentResponse recordDataSharingConsent(ConsentRequest request) {
        Consent consent = consentMapper.toFhir(request);
        MethodOutcome outcome = fhirClient.create().resource(consent).execute();
        consent.setId(outcome.getId());
        return consentMapper.toResponse(consent);
    }

    @Override
    public ConsentResponse revokeConsent(String consentFhirId) {
        Consent consent = readOrThrow(consentFhirId);
        consent.setStatus(ConsentState.INACTIVE);

        // Close the provision period at "now" rather than leaving an open-ended
        // grant, so a subsequent hasActiveConsentForPartner check for a past
        // instant still reflects the truth at that point in time.
        if (consent.getProvision().hasPeriod()) {
            consent.getProvision().getPeriod().setEnd(Date.from(Instant.now()));
        }

        fhirClient.update().resource(consent).execute();
        return consentMapper.toResponse(consent);
    }

    @Override
    public List<ConsentResponse> getActiveConsentsForPatient(String patientFhirId) {
        Bundle bundle = searchActiveConsents(patientFhirId);
        return bundle.getEntry().stream()
                .map(entry -> (Consent) entry.getResource())
                .map(consentMapper::toResponse)
                .toList();
    }

    @Override
    public boolean hasActiveConsentForPartner(String patientFhirId, String partnerOrganizationId) {
        Bundle bundle = searchActiveConsents(patientFhirId);
        Instant now = Instant.now();

        return bundle.getEntry().stream()
                .map(entry -> (Consent) entry.getResource())
                .filter(consent -> consent.hasProvision())
                .filter(consent -> coversInstant(consent.getProvision(), now))
                .anyMatch(consent -> consent.getProvision().getActor().stream()
                        .anyMatch(actor -> actor.hasReference()
                                && actor.getReference().hasIdentifier()
                                && partnerOrganizationId.equals(actor.getReference().getIdentifier().getValue())));
    }

    private boolean coversInstant(Consent.ProvisionComponent provision, Instant instant) {
        if (!provision.hasPeriod()) {
            return true;
        }
        var period = provision.getPeriod();
        boolean afterStart = !period.hasStart() || !instant.isBefore(period.getStart().toInstant());
        boolean beforeEnd = !period.hasEnd() || !instant.isAfter(period.getEnd().toInstant());
        return afterStart && beforeEnd;
    }

    private Bundle searchActiveConsents(String patientFhirId) {
        return fhirClient.search()
                .forResource(Consent.class)
                .where(new ReferenceClientParam("patient").hasId(patientFhirId))
                .and(new TokenClientParam("status").exactly().code("active"))
                .returnBundle(Bundle.class)
                .execute();
    }

    private Consent readOrThrow(String consentFhirId) {
        try {
            return fhirClient.read().resource(Consent.class).withId(consentFhirId).execute();
        } catch (ResourceNotFoundException ex) {
            throw new FhirResourceNotFoundException("Consent", consentFhirId);
        }
    }
}
