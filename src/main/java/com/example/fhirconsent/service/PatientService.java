package com.example.fhirconsent.service;

import com.example.fhirconsent.dto.PatientRequest;
import com.example.fhirconsent.dto.PatientResponse;

import java.util.Optional;

public interface PatientService {

    PatientResponse createPatient(PatientRequest request);

    PatientResponse getPatient(String fhirId);

    Optional<PatientResponse> findByMedicalRecordNumber(String mrn);
}
