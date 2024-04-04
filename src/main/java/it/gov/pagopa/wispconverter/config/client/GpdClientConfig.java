package it.gov.pagopa.wispconverter.config.client;

import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.client.gpd.GpdClient;
import it.gov.pagopa.wispconverter.util.client.gpd.GpdClientLogging;
import it.gov.pagopa.wispconverter.util.client.gpd.GpdClientResponseErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class GpdClientConfig {

    @Value("${client.gpd.read-timeout}")
    private Integer readTimeout;

    @Value("${client.gpd.connect-timeout}")
    private Integer connectTimeout;

    @Value("${client.gpd.base-path}")
    private String basePath;

    @Value("${client.gpd.api-key}")
    private String apiKey;

    @Bean
    public GpdClient gpdClient(ReService reService) {
        GpdClient client = new GpdClient(readTimeout, connectTimeout);
        client.addCustomLoggingInterceptor(new GpdClientLogging());
        client.addCustomErrorHandler(new GpdClientResponseErrorHandler());

        client.setBasePath(basePath);
        client.setBasePath(apiKey);

        return client;
    }
}
