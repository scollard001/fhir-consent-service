package com.example.fhirconsent.exception;

/**
 * Thrown when an external partner attempts to read patient/device data but no
 * active, in-scope Consent grants them permission to do so.
 */
public class ConsentRequiredException extends RuntimeException {

    public ConsentRequiredException(String patientId, String partnerOrganizationId) {
        super("No active consent grants partner '%s' access to patient '%s' data"
                .formatted(partnerOrganizationId, patientId));
    }
}
