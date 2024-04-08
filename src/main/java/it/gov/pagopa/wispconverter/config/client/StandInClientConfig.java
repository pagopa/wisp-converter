package it.gov.pagopa.wispconverter.config.client;

import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.client.MDCInterceptor;
import it.gov.pagopa.wispconverter.util.client.ReInterceptor;
import it.gov.pagopa.wispconverter.util.client.apiconfigcache.ApiConfigCacheClientLoggingInterceptor;
import it.gov.pagopa.wispconverter.util.client.apiconfigcache.ApiConfigCacheClientResponseErrorHandler;
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
public class StandInClientConfig {
    private final ReService reService;

    @Value("${client.standin.read-timeout}")
    private Integer readTimeout;

    @Value("${client.standin.connect-timeout}")
    private Integer connectTimeout;

    @Value("${client.standin.base-path}")
    private String basePath;

    @Value("${client.standin.api-key}")
    private String apiKey;

    @Value("${log.client.standin.request.include-headers}")
    private boolean clientRequestIncludeHeaders;
    @Value("${log.client.standin.request.include-payload}")
    private boolean clientRequestIncludePayload;
    @Value("${log.client.standin.request.max-payload-length}")
    private int clientRequestMaxLength;
    @Value("${log.client.standin.response.include-headers}")
    private boolean clientResponseIncludeHeaders;
    @Value("${log.client.standin.response.include-payload}")
    private boolean clientResponseIncludePayload;
    @Value("${log.client.standin.response.max-payload-length}")
    private int clientResponseMaxLength;

    @Value("${log.client.standin.mask.header.name}")
    private String maskHeaderName;

    @Value("${log.client.standin.request.pretty}")
    private boolean clientRequestPretty;

    @Value("${log.client.standin.response.pretty}")
    private boolean clientResponsePretty;


    @Bean(name="standinClient")
    public it.gov.pagopa.gen.wispconverter.client.standin.invoker.ApiClient standinClient() {
        ApiConfigCacheClientLoggingInterceptor clientLogging = new ApiConfigCacheClientLoggingInterceptor();
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
        currentInterceptors.add(new MDCInterceptor());
        currentInterceptors.add(new ReInterceptor(reService));
        currentInterceptors.add(clientLogging);
        restTemplate.setInterceptors(currentInterceptors);

        restTemplate.setErrorHandler(new ApiConfigCacheClientResponseErrorHandler());

        it.gov.pagopa.gen.wispconverter.client.standin.invoker.ApiClient client = new it.gov.pagopa.gen.wispconverter.client.standin.invoker.ApiClient(restTemplate);
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