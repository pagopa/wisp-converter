package it.gov.pagopa.wispconverter.config.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IUVGeneratorFeignConfig extends AuthFeignConfig {

    private static final String IUVGENERATOR_SUBKEY_PLACEHOLDER = "${client.iuv-generator.subscription-key}";

    @Autowired
    public IUVGeneratorFeignConfig(@Value(IUVGENERATOR_SUBKEY_PLACEHOLDER) String subscriptionKey) {
        this.subscriptionKey = subscriptionKey;
    }
}