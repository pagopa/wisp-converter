package it.gov.pagopa.wispconverter.util.client.decouplercaching;

import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.List;

public class DecouplerCachingClient extends it.gov.pagopa.decouplercachingclient.client.ApiClient {

    private final RestTemplate restTemplate;
    private final Integer readTimeout;
    private final Integer connectTimeout;

    public DecouplerCachingClient(Integer readTimeout, Integer connectTimeout) {
        this.readTimeout = readTimeout;
        this.connectTimeout = connectTimeout;
        this.restTemplate = buildRestTemplate();
        init();
    }

    @Override
    protected RestTemplate buildRestTemplate() {
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

    public void addCustomLoggingInterceptor(ClientHttpRequestInterceptor interceptor) {
        List<ClientHttpRequestInterceptor> currentInterceptors = this.restTemplate.getInterceptors();

        currentInterceptors.add(interceptor);
        this.restTemplate.setInterceptors(currentInterceptors);
    }

    public void addCustomErrorHandler(ResponseErrorHandler errorHandler) {
        this.restTemplate.setErrorHandler(errorHandler);
    }


}
