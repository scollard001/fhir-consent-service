package com.example.fhirconsent.service.impl;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.example.fhirconsent.dto.PatientRequest;
import com.example.fhirconsent.dto.PatientResponse;
import com.example.fhirconsent.exception.FhirResourceNotFoundException;
import com.example.fhirconsent.mapper.PatientMapper;
import com.example.fhirconsent.service.PatientService;
import com.example.fhirconsent.util.FhirSystems;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MethodOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PatientServiceImpl implements PatientService {

    private final IGenericClient fhirClient;
    private final PatientMapper patientMapper;

    public PatientServiceImpl(IGenericClient fhirClient, PatientMapper patientMapper) {
        this.fhirClient = fhirClient;
        this.patientMapper = patientMapper;
    }

    @Override
    public PatientResponse createPatient(PatientRequest request) {
        Patient patient = patientMapper.toFhir(request);
        MethodOutcome outcome = fhirClient.create().resource(patient).execute();
        patient.setId(outcome.getId());
        return patientMapper.toResponse(patient);
    }

    @Override
    public PatientResponse getPatient(String fhirId) {
        try {
            Patient patient = fhirClient.read().resource(Patient.class).withId(fhirId).execute();
            return patientMapper.toResponse(patient);
        } catch (ResourceNotFoundException ex) {
            throw new FhirResourceNotFoundException("Patient", fhirId);
        }
    }

    @Override
    public Optional<PatientResponse> findByMedicalRecordNumber(String mrn) {
        Bundle bundle = fhirClient.search()
                .forResource(Patient.class)
                .where(new TokenClientParam("identifier").exactly().systemAndCode(FhirSystems.PATIENT_MRN, mrn))
                .returnBundle(Bundle.class)
                .execute();

        return bundle.getEntry().stream()
                .map(entry -> (Patient) entry.getResource())
                .findFirst()
                .map(patientMapper::toResponse);
    }
}
