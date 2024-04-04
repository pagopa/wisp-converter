package it.gov.pagopa.wispconverter.config.client;

import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.client.gpd.GpdClientLogging;
import it.gov.pagopa.wispconverter.util.client.iuvgenerator.IuvGeneratorClient;
import it.gov.pagopa.wispconverter.util.client.iuvgenerator.IuvGeneratorClientLogging;
import it.gov.pagopa.wispconverter.util.client.iuvgenerator.IuvGeneratorClientResponseErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class IuvGeneratorClientConfig {

    @Value("${client.iuvgenerator.read-timeout}")
    private Integer readTimeout;

    @Value("${client.iuvgenerator.connect-timeout}")
    private Integer connectTimeout;

    @Value("${client.iuvgenerator.base-path}")
    private String basePath;

    @Value("${client.iuvgenerator.api-key}")
    private String apiKey;


    @Bean
    public IuvGeneratorClient iuvGeneratorClient(ReService reService) {
        IuvGeneratorClient client = new IuvGeneratorClient(readTimeout, connectTimeout);
        client.addCustomLoggingInterceptor(new IuvGeneratorClientLogging());
        client.addCustomErrorHandler(new IuvGeneratorClientResponseErrorHandler());

        client.setBasePath(basePath);
        client.setBasePath(apiKey);

        return client;
    }
}
