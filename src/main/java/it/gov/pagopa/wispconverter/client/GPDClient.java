package it.gov.pagopa.wispconverter.client;

import feign.FeignException;
import it.gov.pagopa.wispconverter.config.client.GPDFeignConfig;
import it.gov.pagopa.wispconverter.model.client.gpd.MultiplePaymentPosition;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "gpd", url = "${client.gpd.host}", configuration = GPDFeignConfig.class)
public interface GPDClient {

    @Retryable(
            noRetryFor = FeignException.FeignClientException.class,
            maxAttemptsExpression = "${client.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${client.retry.max-delay}"))
    @PostMapping(
            value = "${client.gpd.api.bulk-insert.path}",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    void executeBulkCreation(@RequestParam("organization-fiscal-code") String organizationFiscalCode,
                             @RequestBody MultiplePaymentPosition body);
}