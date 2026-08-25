package com.example.fhirconsent.exception;

public class FhirResourceNotFoundException extends RuntimeException {

    public FhirResourceNotFoundException(String resourceType, String id) {
        super("%s/%s was not found".formatted(resourceType, id));
    }
}
