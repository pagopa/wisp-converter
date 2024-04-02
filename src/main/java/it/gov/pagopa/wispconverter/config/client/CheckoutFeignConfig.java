package it.gov.pagopa.wispconverter.config.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckoutFeignConfig extends AuthFeignConfig {

    @Value("${client.checkout.subscription-key}")
    private String subscriptionKey;

    @Override
    public String getSubscriptionKey() {
        return this.subscriptionKey;
    }
}