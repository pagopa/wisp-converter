package it.gov.pagopa.wispconverter.client;

import feign.FeignException;
import it.gov.pagopa.wispconverter.config.client.IUVGeneratorFeignConfig;
import it.gov.pagopa.wispconverter.model.client.iuvgenerator.IUVGeneratorRequest;
import it.gov.pagopa.wispconverter.model.client.iuvgenerator.IUVGeneratorResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "iuv-generator", url = "${client.iuv-generator.host}", configuration = IUVGeneratorFeignConfig.class)
public interface IUVGeneratorClient {

    @Retryable(
            noRetryFor = FeignException.FeignClientException.class,
            maxAttemptsExpression = "${client.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${client.retry.max-delay}"))
    @PostMapping(
            value = "${client.iuv-generator.api.generate.path}",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    IUVGeneratorResponse generate(@RequestParam("organization-fiscal-code") String organizationFiscalCode,
                                  @RequestBody IUVGeneratorRequest body);
}