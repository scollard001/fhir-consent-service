package com.example.fhirconsent.mapper;

import com.example.fhirconsent.dto.DeviceResponse;
import com.example.fhirconsent.dto.InsulinPumpDeviceRequest;
import com.example.fhirconsent.util.FhirSystems;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceMapperTest {

    private final DeviceMapper mapper = new DeviceMapper();

    @Test
    void toFhir_setsPatientReferenceAndUdiFields() {
        InsulinPumpDeviceRequest request = new InsulinPumpDeviceRequest(
                "patient-42",
                "Acme MedTech",
                "AP-2000",
                "SN-778812",
                "3.4.1",
                "LOT-55",
                LocalDate.of(2024, 6, 1),
                "(01)00844588003288(11)240601(17)290601(10)LOT-55(21)SN-778812"
        );

        Device device = mapper.toFhir(request);

        assertThat(device.getPatient().getReference()).isEqualTo("Patient/patient-42");
        assertThat(device.getTypeFirstRep().getCodingFirstRep().getCode())
                .isEqualTo(FhirSystems.SNOMED_INSULIN_PUMP_DEVICE);
        assertThat(device.getSerialNumber()).isEqualTo("SN-778812");
        assertThat(device.getUdiCarrierFirstRep().getCarrierHRF()).contains("SN-778812");
        assertThat(device.getStatus().toCode()).isEqualTo("active");
    }

    @Test
    void toResponse_extractsSoftwareVersionAndUdi() {
        Device device = mapper.toFhir(new InsulinPumpDeviceRequest(
                "patient-42", "Acme MedTech", "AP-2000", "SN-1",
                "1.0.0", "LOT-1", LocalDate.of(2023, 1, 1), "udi-hrf-value"));
        device.setId(new IdType("Device", "dev-1"));

        DeviceResponse response = mapper.toResponse(device);

        assertThat(response.fhirId()).isEqualTo("dev-1");
        assertThat(response.patientFhirId()).isEqualTo("patient-42");
        assertThat(response.softwareVersion()).isEqualTo("1.0.0");
        assertThat(response.udiCarrierHrf()).isEqualTo("udi-hrf-value");
    }
}
