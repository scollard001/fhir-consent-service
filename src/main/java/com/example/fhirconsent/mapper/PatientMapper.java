package com.example.fhirconsent.mapper;

import com.example.fhirconsent.dto.PatientRequest;
import com.example.fhirconsent.dto.PatientResponse;
import com.example.fhirconsent.util.FhirSystems;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

@Component
public class PatientMapper {

    public Patient toFhir(PatientRequest request) {
        Patient patient = new Patient();

        HumanName name = new HumanName().setFamily(request.familyName());
        request.givenNames().forEach(name::addGiven);
        patient.addName(name);

        patient.setGender(toFhirGender(request.gender()));
        patient.setBirthDate(Date.from(request.birthDate().atStartOfDay(ZoneOffset.UTC).toInstant()));

        patient.addIdentifier(new Identifier()
                .setSystem(FhirSystems.PATIENT_MRN)
                .setValue(request.medicalRecordNumber()));

        return patient;
    }

    public PatientResponse toResponse(Patient patient) {
        HumanName name = patient.getNameFirstRep();
        String mrn = patient.getIdentifier().stream()
                .filter(id -> FhirSystems.PATIENT_MRN.equals(id.getSystem()))
                .map(Identifier::getValue)
                .findFirst()
                .orElse(null);

        return new PatientResponse(
                patient.getIdElement().getIdPart(),
                name.getFamily(),
                name.getGiven().stream().map(g -> g.getValueAsString()).toList(),
                patient.getBirthDate() == null
                        ? null
                        : patient.getBirthDate().toInstant().atZone(ZoneId.of("UTC")).toLocalDate(),
                patient.getGender() == null ? null : patient.getGender().toCode(),
                mrn
        );
    }

    private AdministrativeGender toFhirGender(PatientRequest.AdministrativeGender gender) {
        return switch (gender) {
            case MALE -> AdministrativeGender.MALE;
            case FEMALE -> AdministrativeGender.FEMALE;
            case OTHER -> AdministrativeGender.OTHER;
            case UNKNOWN -> AdministrativeGender.UNKNOWN;
        };
    }
}
