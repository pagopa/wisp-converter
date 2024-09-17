package it.gov.pagopa.wispconverter.config.client;

import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.client.RequestResponseLoggingProperties;
import it.gov.pagopa.wispconverter.util.client.iuvgenerator.IuvGeneratorClientLoggingInterceptor;
import it.gov.pagopa.wispconverter.util.client.iuvgenerator.IuvGeneratorClientResponseErrorHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class IuvGeneratorClientConfig {

    private final ReService reService;

    @Value("${client.iuvgenerator.read-timeout}")
    private Integer readTimeout;

    @Value("${client.iuvgenerator.connect-timeout}")
    private Integer connectTimeout;

    @Value("${client.iuvgenerator.base-path}")
    private String basePath;

    @Value("${client.iuvgenerator.api-key}")
    private String apiKey;

    @Value("${wisp-converter.re-tracing.interface.iuv-generator.enabled}")
    private Boolean isTracingOfClientOnREEnabled;

    @Bean
    @ConfigurationProperties(prefix = "log.client.iuvgenerator")
    public RequestResponseLoggingProperties iuvGeneratorClientLoggingProperties() {
        return new RequestResponseLoggingProperties();
    }


    @Bean
    public it.gov.pagopa.gen.wispconverter.client.iuvgenerator.invoker.ApiClient iuvGeneratorClient() {
        RequestResponseLoggingProperties clientLoggingProperties = iuvGeneratorClientLoggingProperties();

        IuvGeneratorClientLoggingInterceptor clientLogging = new IuvGeneratorClientLoggingInterceptor(clientLoggingProperties, reService, isTracingOfClientOnREEnabled);

        RestTemplate restTemplate = restTemplate();

        List<ClientHttpRequestInterceptor> currentInterceptors = restTemplate.getInterceptors();
        currentInterceptors.add(clientLogging);
        restTemplate.setInterceptors(currentInterceptors);

        restTemplate.setErrorHandler(new IuvGeneratorClientResponseErrorHandler());

        it.gov.pagopa.gen.wispconverter.client.iuvgenerator.invoker.ApiClient client = new it.gov.pagopa.gen.wispconverter.client.iuvgenerator.invoker.ApiClient(restTemplate);
        client.setBasePath(basePath);
        client.setApiKey(apiKey);

        return client;
    }

    private RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // This allows us to read the response more than once - Necessary for debugging.
        restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(getSimpleClientHttpRequestFactory(restTemplate.getRequestFactory())));

        // disable default URL encoding
        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
        uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
        restTemplate.setUriTemplateHandler(uriBuilderFactory);
        return restTemplate;
    }

    private SimpleClientHttpRequestFactory getSimpleClientHttpRequestFactory(ClientHttpRequestFactory requestFactory) {
        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = (SimpleClientHttpRequestFactory) requestFactory;
        simpleClientHttpRequestFactory.setConnectTimeout(this.connectTimeout);
        simpleClientHttpRequestFactory.setReadTimeout(this.readTimeout);
        return simpleClientHttpRequestFactory;
    }
}