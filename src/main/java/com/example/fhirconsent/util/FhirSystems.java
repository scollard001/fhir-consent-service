package com.example.fhirconsent.util;

/**
 * Central registry of the URIs/codesystems this service uses when constructing
 * or reading FHIR resources. Keeping these in one place avoids "magic string"
 * drift between the mapper classes and makes it obvious, at a glance, which
 * identifier systems and code systems are project-specific (https://example.org/...)
 * versus standard terminology (SNOMED CT, HL7 Terminology).
 */
public final class FhirSystems {

    private FhirSystems() {
    }

    // --- Identifier systems (project-specific) ---------------------------------
    public static final String PATIENT_MRN = "https://example.org/fhir/identifiers/mrn";
    public static final String DEVICE_SERIAL = "https://example.org/fhir/identifiers/device-serial";
    public static final String PARTNER_ORG_ID = "https://example.org/fhir/identifiers/partner-org";

    // --- Standard terminology ----------------------------------------------------
    public static final String SNOMED_CT = "http://snomed.info/sct";
    public static final String CONSENT_SCOPE = "http://terminology.hl7.org/CodeSystem/consentscope";
    public static final String CONSENT_CATEGORY = "http://terminology.hl7.org/CodeSystem/consentcategorycodes";
    public static final String V3_ACT_REASON = "http://terminology.hl7.org/CodeSystem/v3-ActReason";
    public static final String V3_PARTICIPATION_TYPE = "http://terminology.hl7.org/CodeSystem/v3-ParticipationType";

    // --- Project-specific value sets ---------------------------------------------
    // A real deployment would publish this as a proper FHIR CodeSystem/ValueSet
    // rather than an ad-hoc string constant.
    public static final String DATA_CATEGORY = "https://example.org/fhir/CodeSystem/data-categories";

    /** SNOMED CT: "Insulin pump device (physical object)". */
    public static final String SNOMED_INSULIN_PUMP_DEVICE = "469756000";
}
