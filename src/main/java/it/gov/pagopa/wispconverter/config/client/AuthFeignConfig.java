package it.gov.pagopa.wispconverter.config.client;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;

public abstract class AuthFeignConfig {

    static final String HEADER_REQUEST_ID = "X-Request-Id";
    static final String HEADER_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";
    protected String subscriptionKey;

    @Bean
    public RequestInterceptor requestIdInterceptor() {
        return requestTemplate -> requestTemplate
                .header(HEADER_REQUEST_ID, MDC.get("requestId"))
                .header(HEADER_SUBSCRIPTION_KEY, subscriptionKey);
    }
}