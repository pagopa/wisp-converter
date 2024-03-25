package it.gov.pagopa.wispconverter.config.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GPDFeignConfig extends AuthFeignConfig {

    private static final String GPD_SUBKEY_PLACEHOLDER = "${client.gpd.subscription-key}";

    @Autowired
    public GPDFeignConfig(@Value(GPD_SUBKEY_PLACEHOLDER) String subscriptionKey) {
        this.subscriptionKey = subscriptionKey;
    }
}