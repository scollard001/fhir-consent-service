package com.example.fhirconsent.service;

import com.example.fhirconsent.dto.DeviceResponse;
import com.example.fhirconsent.dto.InsulinPumpDeviceRequest;

import java.util.List;

public interface DeviceService {

    DeviceResponse registerInsulinPump(InsulinPumpDeviceRequest request);

    List<DeviceResponse> getDevicesForPatient(String patientFhirId);
}
