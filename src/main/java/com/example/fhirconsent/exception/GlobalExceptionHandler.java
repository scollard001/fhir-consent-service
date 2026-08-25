package com.example.fhirconsent.exception;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates internal and HAPI FHIR client exceptions into RFC 7807
 * {@link ProblemDetail} responses so callers get a stable, documented error
 * contract instead of a leaking stack trace or a raw FHIR OperationOutcome.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FhirResourceNotFoundException.class)
    public ProblemDetail handleNotFound(FhirResourceNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConsentRequiredException.class)
    public ProblemDetail handleConsentRequired(ConsentRequiredException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        detail.setTitle("Consent required");
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(DataFormatException.class)
    public ProblemDetail handleFhirDataFormat(DataFormatException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Malformed FHIR resource: " + ex.getMessage());
    }

    @ExceptionHandler(BaseServerResponseException.class)
    public ProblemDetail handleFhirServerError(BaseServerResponseException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status,
                "Upstream FHIR server rejected the request: " + ex.getMessage());
        detail.setTitle("FHIR server error");
        return detail;
    }
}
