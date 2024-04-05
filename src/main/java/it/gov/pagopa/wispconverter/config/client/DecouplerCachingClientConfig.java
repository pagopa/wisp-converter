package it.gov.pagopa.wispconverter.config.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.client.decouplercaching.DecouplerCachingClientLoggingInterceptor;
import it.gov.pagopa.wispconverter.util.client.decouplercaching.DecouplerCachingClientResponseErrorHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class DecouplerCachingClientConfig {

    private final ReService reService;

    @Value("${client.decoupler-caching.read-timeout}")
    private Integer readTimeout;

    @Value("${client.decoupler-caching.connect-timeout}")
    private Integer connectTimeout;

    @Value("${client.decoupler-caching.base-path}")
    private String basePath;

    @Value("${client.decoupler-caching.api-key}")
    private String apiKey;

    @Value("${log.client.decoupler-caching.request.include-headers}")
    private boolean clientRequestIncludeHeaders;
    @Value("${log.client.decoupler-caching.request.include-payload}")
    private boolean clientRequestIncludePayload;
    @Value("${log.client.decoupler-caching.request.max-payload-length}")
    private int clientRequestMaxLength;
    @Value("${log.client.decoupler-caching.response.include-headers}")
    private boolean clientResponseIncludeHeaders;
    @Value("${log.client.decoupler-caching.response.include-payload}")
    private boolean clientResponseIncludePayload;
    @Value("${log.client.decoupler-caching.response.max-payload-length}")
    private int clientResponseMaxLength;

    @Value("${log.client.decoupler-caching.mask.header.name}")
    private String maskHeaderName;

    @Value("${log.client.decoupler-caching.request.pretty}")
    private boolean clientRequestPretty;

    @Value("${log.client.decoupler-caching.response.pretty}")
    private boolean clientResponsePretty;

    @Bean
    public it.gov.pagopa.gen.wispconverter.client.decouplercaching.invoker.ApiClient decouplerCachingClient() {
        DecouplerCachingClientLoggingInterceptor clientLogging = new DecouplerCachingClientLoggingInterceptor(reService);
        clientLogging.setRequestIncludeHeaders(clientRequestIncludeHeaders);
        clientLogging.setRequestIncludePayload(clientRequestIncludePayload);
        clientLogging.setRequestMaxPayloadLength(clientRequestMaxLength);
        clientLogging.setRequestHeaderPredicate(p -> !p.equals(maskHeaderName));
        clientLogging.setRequestPretty(clientRequestPretty);

        clientLogging.setResponseIncludeHeaders(clientResponseIncludeHeaders);
        clientLogging.setResponseIncludePayload(clientResponseIncludePayload);
        clientLogging.setResponseMaxPayloadLength(clientResponseMaxLength);
        clientLogging.setResponsePretty(clientResponsePretty);

        RestTemplate restTemplate = restTemplate();

        List<ClientHttpRequestInterceptor> currentInterceptors = restTemplate.getInterceptors();
        currentInterceptors.add(clientLogging);
        restTemplate.setInterceptors(currentInterceptors);

        restTemplate.setErrorHandler(new DecouplerCachingClientResponseErrorHandler());

        it.gov.pagopa.gen.wispconverter.client.decouplercaching.invoker.ApiClient client = new it.gov.pagopa.gen.wispconverter.client.decouplercaching.invoker.ApiClient(restTemplate);

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