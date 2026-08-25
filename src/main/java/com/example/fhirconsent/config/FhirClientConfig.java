package com.example.fhirconsent.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires up the HAPI FHIR client used to talk to the FHIR store.
 *
 * <p>We hold a single, shared {@link FhirContext}: per HAPI's own guidance it is
 * expensive to construct (it scans the resource model on creation) but is
 * thread-safe, so it is created exactly once per JVM and reused for every
 * request.</p>
 */
@Configuration
@EnableConfigurationProperties(FhirClientConfig.FhirProperties.class)
public class FhirClientConfig {

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Bean
    public IGenericClient fhirClient(FhirContext fhirContext, FhirProperties properties) {
        fhirContext.getRestfulClientFactory().setSocketTimeout(properties.getClient().getSocketTimeoutMs());
        fhirContext.getRestfulClientFactory().setConnectTimeout(properties.getClient().getConnectTimeoutMs());

        // NEVER: don't block application startup (or every request) on fetching
        // the server's CapabilityStatement. The trade-off is that a genuinely
        // incompatible server version fails on first real call instead of at
        // client construction time - acceptable for a service that already has
        // integration tests pinned to a known server image.
        fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);

        IGenericClient client = fhirContext.newRestfulGenericClient(properties.getServer().getBaseUrl());

        if (properties.getClient().isLogRequestsAndResponses()) {
            client.registerInterceptor(new LoggingInterceptor(true));
        }

        return client;
    }

    @Getter
    @Setter
    @ConfigurationProperties(prefix = "fhir")
    public static class FhirProperties {
        private Server server = new Server();
        private Client client = new Client();

        @Getter
        @Setter
        public static class Server {
            private String baseUrl;
        }

        @Getter
        @Setter
        public static class Client {
            private boolean logRequestsAndResponses = false;
            private int socketTimeoutMs = 30_000;
            private int connectTimeoutMs = 5_000;
        }
    }
}
