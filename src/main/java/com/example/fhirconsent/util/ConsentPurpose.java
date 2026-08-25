package com.example.fhirconsent.util;

/**
 * Subset of HL7 v3 ActReason codes relevant to sharing device/health data with
 * an external partner. Kept intentionally small for this example; extend as
 * new partner integration types are onboarded.
 */
public enum ConsentPurpose {

    /** Treatment - e.g. a remote care management vendor monitoring device readings. */
    TREATMENT("TREAT"),
    /** Healthcare research use of de-identified or identified data. */
    RESEARCH("HRESCH"),
    /** Care management / population health analytics. */
    CARE_MANAGEMENT("CAREMGT");

    private final String v3ActReasonCode;

    ConsentPurpose(String v3ActReasonCode) {
        this.v3ActReasonCode = v3ActReasonCode;
    }

    public String v3ActReasonCode() {
        return v3ActReasonCode;
    }
}
