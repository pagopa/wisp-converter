package it.gov.pagopa.wispconverter.config.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.client.gpd.GpdClientLoggingInterceptor;
import it.gov.pagopa.wispconverter.util.client.gpd.GpdClientResponseErrorHandler;
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
public class GpdClientConfig {

    private final ReService reService;

    @Value("${client.gpd.read-timeout}")
    private Integer readTimeout;

    @Value("${client.gpd.connect-timeout}")
    private Integer connectTimeout;

    @Value("${client.gpd.base-path}")
    private String basePath;

    @Value("${client.gpd.api-key}")
    private String apiKey;


    @Bean
    @ConfigurationProperties(prefix = "log.client.gpd")
    public ClientLoggingProperties gpdClientLoggingProperties() {
        return new ClientLoggingProperties();
    }

    @Bean
    public it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient gpdClient() {
        ClientLoggingProperties clientLoggingProperties = gpdClientLoggingProperties();

        GpdClientLoggingInterceptor clientLogging = new GpdClientLoggingInterceptor(reService);
        clientLogging.setRequestIncludeHeaders(clientLoggingProperties.getRequest().isIncludeHeaders());
        clientLogging.setRequestIncludePayload(clientLoggingProperties.getRequest().isIncludePayload());
        clientLogging.setRequestMaxPayloadLength(clientLoggingProperties.getRequest().getMaxPayloadLength());
        clientLogging.setRequestHeaderPredicate(p -> !p.equals(clientLoggingProperties.getRequest().getMaskHeaderName()));
        clientLogging.setRequestPretty(clientLoggingProperties.getRequest().isPretty());

        clientLogging.setResponseIncludeHeaders(clientLoggingProperties.getResponse().isIncludeHeaders());
        clientLogging.setResponseIncludePayload(clientLoggingProperties.getResponse().isIncludePayload());
        clientLogging.setResponseMaxPayloadLength(clientLoggingProperties.getResponse().getMaxPayloadLength());
        clientLogging.setResponsePretty(clientLoggingProperties.getResponse().isPretty());

        RestTemplate restTemplate = restTemplate();

        List<ClientHttpRequestInterceptor> currentInterceptors = restTemplate.getInterceptors();
        currentInterceptors.add(clientLogging);
        restTemplate.setInterceptors(currentInterceptors);

        restTemplate.setErrorHandler(new GpdClientResponseErrorHandler());

        it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient client = new it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient(restTemplate);

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