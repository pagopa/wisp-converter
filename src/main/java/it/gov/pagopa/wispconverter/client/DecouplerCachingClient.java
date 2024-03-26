package it.gov.pagopa.wispconverter.client;

import feign.FeignException;
import it.gov.pagopa.wispconverter.config.client.DecouplerCachingFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(value = "decoupler-caching", url = "${client.decoupler-caching.host}", configuration = DecouplerCachingFeignConfig.class)
public interface DecouplerCachingClient {

    @Retryable(
            noRetryFor = FeignException.FeignClientException.class,
            maxAttemptsExpression = "${client.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${client.retry.max-delay}"))
    @PostMapping(
            value = "${client.decoupler-caching.api.store-key.path}",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    void storeKeyInCacheByAPIM(@PathVariable String key);
}