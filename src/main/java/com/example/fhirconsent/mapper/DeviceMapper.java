package com.example.fhirconsent.mapper;

import com.example.fhirconsent.dto.DeviceResponse;
import com.example.fhirconsent.dto.InsulinPumpDeviceRequest;
import com.example.fhirconsent.util.FhirSystems;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Device.DeviceUdiCarrierComponent;
import org.hl7.fhir.r4.model.Device.FHIRDeviceStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * Maps insulin pump device requests to/from the generic FHIR {@code Device}
 * resource. We model the pump as a standard Device with a SNOMED CT type
 * coding rather than a bespoke resource, so downstream consumers of the FHIR
 * store (analytics, other services) can query it via the normal Device
 * search parameters without knowing anything device-specific about pumps.
 */
@Component
public class DeviceMapper {

    public Device toFhir(InsulinPumpDeviceRequest request) {
        Device device = new Device();

        device.setPatient(new Reference("Patient/" + request.patientFhirId()));
        device.setStatus(FHIRDeviceStatus.ACTIVE);

        device.setType(new CodeableConcept().addCoding(new Coding()
                .setSystem(FhirSystems.SNOMED_CT)
                .setCode(FhirSystems.SNOMED_INSULIN_PUMP_DEVICE)
                .setDisplay("Insulin pump device")));

        device.setManufacturer(request.manufacturer());
        device.setModelNumber(request.modelNumber());
        device.setSerialNumber(request.serialNumber());
        device.setLotNumber(request.lotNumber());

        if (request.softwareVersion() != null) {
            device.addVersion(new Device.DeviceVersionComponent().setValue(request.softwareVersion()));
        }

        if (request.manufactureDate() != null) {
            device.setManufactureDate(Date.from(
                    request.manufactureDate().atStartOfDay(ZoneOffset.UTC).toInstant()));
        }

        device.addIdentifier(new Identifier()
                .setSystem(FhirSystems.DEVICE_SERIAL)
                .setValue(request.serialNumber()));

        device.addUdiCarrier(new DeviceUdiCarrierComponent()
                .setCarrierHRF(request.udiCarrierHrf()));

        return device;
    }

    public DeviceResponse toResponse(Device device) {
        String softwareVersion = device.getVersion().isEmpty()
                ? null
                : device.getVersion().get(0).getValue();

        String udiHrf = device.getUdiCarrier().isEmpty()
                ? null
                : device.getUdiCarrier().get(0).getCarrierHRF();

        LocalDate manufactureDate = device.getManufactureDate() == null
                ? null
                : device.getManufactureDate().toInstant().atZone(ZoneId.of("UTC")).toLocalDate();

        return new DeviceResponse(
                device.getIdElement().getIdPart(),
                device.getPatient().getReferenceElement().getIdPart(),
                device.getManufacturer(),
                device.getModelNumber(),
                device.getSerialNumber(),
                softwareVersion,
                device.getLotNumber(),
                manufactureDate,
                udiHrf,
                device.getStatus() == null ? null : device.getStatus().toCode()
        );
    }
}
