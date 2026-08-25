package com.example.fhirconsent.mapper;

import com.example.fhirconsent.dto.ConsentRequest;
import com.example.fhirconsent.dto.ConsentResponse;
import com.example.fhirconsent.util.ConsentPurpose;
import com.example.fhirconsent.util.FhirSystems;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.Consent.ConsentState;
import org.hl7.fhir.r4.model.Consent.provisionType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps external-data-sharing consent requests to/from the FHIR {@code Consent}
 * resource.
 *
 * <p>This is a deliberately simplified profile suitable for a demo: a single,
 * top-level {@code provision} granting a named partner organization permission
 * to receive specific categories of data for a stated purpose, over a bounded
 * period. Production systems handling real patient consent should evaluate a
 * proper implementation guide (e.g. an HL7 Consent Management / DS4P-aligned
 * IG) rather than rolling their own provision structure, since consent
 * withdrawal, exception provisions ("permit all except X"), and
 * multi-jurisdiction policy references all have real regulatory weight.</p>
 */
@Component
public class ConsentMapper {

    public Consent toFhir(ConsentRequest request) {
        Consent consent = new Consent();

        consent.setStatus(ConsentState.ACTIVE);
        consent.setPatient(new Reference("Patient/" + request.patientFhirId()));
        consent.setDateTime(Date.from(Instant.now()));

        consent.setScope(new CodeableConcept().addCoding(new Coding()
                .setSystem(FhirSystems.CONSENT_SCOPE)
                .setCode("patient-privacy")
                .setDisplay("Privacy Consent")));

        consent.addCategory(new CodeableConcept().addCoding(new Coding()
                .setSystem(FhirSystems.CONSENT_CATEGORY)
                .setCode("IDSCL")
                .setDisplay("Information Disclosure")));

        Consent.ProvisionComponent provision = new Consent.ProvisionComponent();
        provision.setType(provisionType.PERMIT);
        provision.setPeriod(new Period()
                .setStart(Date.from(request.effectiveFrom().toInstant()))
                .setEnd(request.effectiveTo() == null ? null : Date.from(request.effectiveTo().toInstant())));

        provision.addPurpose(new Coding()
                .setSystem(FhirSystems.V3_ACT_REASON)
                .setCode(request.purpose().v3ActReasonCode()));

        Consent.provisionActorComponent actor = new Consent.provisionActorComponent();
        actor.setRole(new CodeableConcept().addCoding(new Coding()
                .setSystem(FhirSystems.V3_PARTICIPATION_TYPE)
                .setCode("IRCP")
                .setDisplay("Information Recipient")));
        // Logical reference: the partner org is identified by its external ID
        // rather than requiring a FHIR Organization resource to exist in this
        // store. This keeps partner onboarding decoupled from the FHIR store's
        // resource lifecycle.
        actor.setReference(new Reference()
                .setIdentifier(new Identifier()
                        .setSystem(FhirSystems.PARTNER_ORG_ID)
                        .setValue(request.partnerOrganizationId()))
                .setDisplay(request.partnerOrganizationName()));
        provision.addActor(actor);

        request.dataCategories().forEach(category -> provision.addClass_(new Coding()
                .setSystem(FhirSystems.DATA_CATEGORY)
                .setCode(category)));

        consent.setProvision(provision);

        return consent;
    }

    public ConsentResponse toResponse(Consent consent) {
        Consent.ProvisionComponent provision = consent.getProvision();

        String partnerOrgId = provision.getActor().stream()
                .findFirst()
                .map(actor -> actor.getReference().getIdentifier().getValue())
                .orElse(null);

        String partnerOrgName = provision.getActor().stream()
                .findFirst()
                .map(actor -> actor.getReference().getDisplay())
                .orElse(null);

        String purposeCode = provision.getPurpose().stream()
                .findFirst()
                .map(Coding::getCode)
                .orElse(null);

        List<String> dataCategories = provision.getClass_().stream()
                .map(Coding::getCode)
                .collect(Collectors.toList());

        OffsetDateTime start = toOffsetDateTime(provision.getPeriod().getStart());
        OffsetDateTime end = toOffsetDateTime(provision.getPeriod().getEnd());

        return new ConsentResponse(
                consent.getIdElement().getIdPart(),
                consent.getPatient().getReferenceElement().getIdPart(),
                partnerOrgId,
                partnerOrgName,
                resolvePurposeEnum(purposeCode),
                dataCategories,
                start,
                end,
                consent.getStatus() == null ? null : consent.getStatus().toCode()
        );
    }

    private String resolvePurposeEnum(String v3Code) {
        if (v3Code == null) {
            return null;
        }
        return Arrays.stream(ConsentPurpose.values())
                .filter(p -> p.v3ActReasonCode().equals(v3Code))
                .findFirst()
                .map(Enum::name)
                .orElse(v3Code);
    }

    private OffsetDateTime toOffsetDateTime(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.of("UTC")).toOffsetDateTime();
    }
}
