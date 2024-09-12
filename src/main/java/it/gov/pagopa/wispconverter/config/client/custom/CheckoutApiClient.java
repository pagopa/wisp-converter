package it.gov.pagopa.wispconverter.config.client.custom;

import it.gov.pagopa.wispconverter.exception.AppException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class CheckoutApiClient extends it.gov.pagopa.gen.wispconverter.client.checkout.invoker.ApiClient {

    public CheckoutApiClient(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    public <T> ResponseEntity<T> invokeAPI(String path,
                                           HttpMethod method,
                                           Map<String, Object> pathParams,
                                           MultiValueMap<String, String> queryParams,
                                           Object body,
                                           HttpHeaders headerParams,
                                           MultiValueMap<String, String> cookieParams,
                                           MultiValueMap<String, Object> formParams,
                                           List<MediaType> accept,
                                           MediaType contentType,
                                           String[] authNames,
                                           ParameterizedTypeReference<T> returnType)
            throws RestClientException {

        int attempts = 0;
        ResponseEntity<T> response = null;
        while (attempts < super.getMaxAttemptsForRetry()) {
            try {
                response = super.invokeAPI(path, method, pathParams, queryParams, body, headerParams, cookieParams, formParams, accept, contentType, authNames, returnType);
            } catch (AppException ex) {
                Throwable cause = ex.getCause();
                attempts = handleRetry(ex, cause, attempts);
            }
        }
        return response;
    }

    private int handleRetry(AppException ex, Throwable cause, int attempts) {
        if (cause instanceof HttpServerErrorException
                || ((HttpClientErrorException) cause)
                .getStatusCode()
                .equals(HttpStatus.TOO_MANY_REQUESTS)) {
            attempts++;
            if (attempts < super.getMaxAttemptsForRetry()) {
                try {
                    Thread.sleep(super.getWaitTimeMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                throw ex;
            }
        } else {
            throw ex;
        }
        return attempts;
    }


}
