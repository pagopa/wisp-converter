package it.gov.pagopa.wispconverter.config.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DecouplerCachingFeignConfig extends AuthFeignConfig {

    private static final String DECOUPLER_CACHING_SUBSCRIPTION_KEY = "${client.decoupler-caching.subscription-key}";

    @Autowired
    public DecouplerCachingFeignConfig(@Value(DECOUPLER_CACHING_SUBSCRIPTION_KEY) String subscriptionKey) {
        this.subscriptionKey = subscriptionKey;
    }
}