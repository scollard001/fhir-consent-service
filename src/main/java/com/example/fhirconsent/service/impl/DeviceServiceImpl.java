package com.example.fhirconsent.service.impl;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.example.fhirconsent.dto.DeviceResponse;
import com.example.fhirconsent.dto.InsulinPumpDeviceRequest;
import com.example.fhirconsent.exception.FhirResourceNotFoundException;
import com.example.fhirconsent.mapper.DeviceMapper;
import com.example.fhirconsent.service.DeviceService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.MethodOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceServiceImpl implements DeviceService {

    private final IGenericClient fhirClient;
    private final DeviceMapper deviceMapper;

    public DeviceServiceImpl(IGenericClient fhirClient, DeviceMapper deviceMapper) {
        this.fhirClient = fhirClient;
        this.deviceMapper = deviceMapper;
    }

    @Override
    public DeviceResponse registerInsulinPump(InsulinPumpDeviceRequest request) {
        // Fail fast with a clear 404 rather than letting the FHIR server reject
        // the Device create because Device.patient points at a non-existent
        // Patient - the referential integrity error from a generic FHIR store
        // is much less actionable for a caller than ours.
        assertPatientExists(request.patientFhirId());

        Device device = deviceMapper.toFhir(request);
        MethodOutcome outcome = fhirClient.create().resource(device).execute();
        device.setId(outcome.getId());
        return deviceMapper.toResponse(device);
    }

    @Override
    public List<DeviceResponse> getDevicesForPatient(String patientFhirId) {
        assertPatientExists(patientFhirId);

        Bundle bundle = fhirClient.search()
                .forResource(Device.class)
                .where(new ReferenceClientParam("patient").hasId(patientFhirId))
                .returnBundle(Bundle.class)
                .execute();

        return bundle.getEntry().stream()
                .map(entry -> (Device) entry.getResource())
                .map(deviceMapper::toResponse)
                .toList();
    }

    private void assertPatientExists(String patientFhirId) {
        try {
            fhirClient.read().resource(Patient.class).withId(patientFhirId).execute();
        } catch (ResourceNotFoundException ex) {
            throw new FhirResourceNotFoundException("Patient", patientFhirId);
        }
    }
}
