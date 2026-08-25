package com.example.fhirconsent.controller;

import com.example.fhirconsent.dto.DeviceResponse;
import com.example.fhirconsent.dto.InsulinPumpDeviceRequest;
import com.example.fhirconsent.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients/{patientFhirId}/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceResponse registerInsulinPump(@PathVariable String patientFhirId,
                                               @Valid @RequestBody InsulinPumpDeviceRequest request) {
        // patientFhirId in the path is authoritative; ignore/overwrite whatever
        // the body claims to avoid a path/body mismatch bug class entirely.
        InsulinPumpDeviceRequest scoped = new InsulinPumpDeviceRequest(
                patientFhirId,
                request.manufacturer(),
                request.modelNumber(),
                request.serialNumber(),
                request.softwareVersion(),
                request.lotNumber(),
                request.manufactureDate(),
                request.udiCarrierHrf()
        );
        return deviceService.registerInsulinPump(scoped);
    }

    @GetMapping
    public List<DeviceResponse> getDevicesForPatient(@PathVariable String patientFhirId) {
        return deviceService.getDevicesForPatient(patientFhirId);
    }
}
