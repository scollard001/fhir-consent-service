import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.example"
version = "0.1.0"
description = "Reference Spring Boot service demonstrating Patient / Device / Consent " +
        "management on top of a HAPI FHIR server, modeling an insulin pump device " +
        "associated with a patient and the patient's consent to share device data " +
        "with external partners."

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

val hapiFhirVersion = "7.4.0"
val testcontainersVersion = "1.20.1"

dependencies {
    // Core web / validation / observability
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // HAPI FHIR client + R4 model. We deliberately depend on the *client*
    // library only: this service is a business-logic facade in front of a
    // separately deployed FHIR store, not an embedded FHIR server.
    // See docs/adr/0001-fhir-client-vs-embedded-server.md
    implementation("ca.uhn.hapi.fhir:hapi-fhir-base:$hapiFhirVersion")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-client:$hapiFhirVersion")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:$hapiFhirVersion")
    // Pulled in transitively by hapi-fhir-client but pinned explicitly since
    // we rely on it directly for logging interceptors in tests.
    implementation("org.apache.httpcomponents.client5:httpclient5")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Lombok is compileOnly and never ends up in the executable jar; no extra
// exclusion is needed here the way the old pom.xml had to configure the
// Maven Shade/Boot plugin explicitly - the Spring Boot Gradle plugin already
// only bundles the runtime classpath.
tasks.named<BootJar>("bootJar") {
    archiveFileName = "fhir-consent-service.jar"
}
