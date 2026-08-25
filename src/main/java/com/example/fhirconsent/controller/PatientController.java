package com.example.fhirconsent.controller;

import com.example.fhirconsent.dto.PatientRequest;
import com.example.fhirconsent.dto.PatientResponse;
import com.example.fhirconsent.exception.FhirResourceNotFoundException;
import com.example.fhirconsent.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse createPatient(@Valid @RequestBody PatientRequest request) {
        return patientService.createPatient(request);
    }

    @GetMapping("/{fhirId}")
    public PatientResponse getPatient(@PathVariable String fhirId) {
        return patientService.getPatient(fhirId);
    }

    @GetMapping
    public ResponseEntity<PatientResponse> findByMrn(@RequestParam String mrn) {
        return patientService.findByMedicalRecordNumber(mrn)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new FhirResourceNotFoundException("Patient", "mrn=" + mrn));
    }
}
