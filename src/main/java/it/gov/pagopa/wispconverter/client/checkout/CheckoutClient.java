package it.gov.pagopa.wispconverter.client.checkout;

import feign.FeignException;
import feign.Response;
import it.gov.pagopa.wispconverter.client.checkout.model.Cart;
import it.gov.pagopa.wispconverter.config.client.CheckoutFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "checkout", url = "${client.checkout.host}", configuration = CheckoutFeignConfig.class)
public interface CheckoutClient {

    @Retryable(
            noRetryFor = FeignException.FeignClientException.class,
            maxAttemptsExpression = "${client.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${client.retry.max-delay}"))
    @PostMapping(
            value = "${client.checkout.api.carts.path}",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    Response executeCreation(@RequestBody Cart body);
}