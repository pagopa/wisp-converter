package it.gov.pagopa.wispconverter.config.client.custom;

import it.gov.pagopa.wispconverter.exception.AppException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.*;

import java.util.List;
import java.util.Map;

public class DecouplerApiClient extends it.gov.pagopa.gen.wispconverter.client.decouplercaching.invoker.ApiClient {

    public DecouplerApiClient(RestTemplate restTemplate) {
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
                break;
            } catch (AppException ex) {
                attempts = handleAppException(ex, attempts);
            } catch (ResourceAccessException ex) {
                attempts = handleRetry(ex, attempts);
            }
        }
        return response;
    }

    private int handleAppException(AppException ex, int attempts) {
        Throwable cause = ex.getCause();
        if (cause instanceof RuntimeException runtimeException) {
            if (cause instanceof HttpServerErrorException || ((HttpClientErrorException) cause).getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS)) {
                attempts = handleRetry(runtimeException, attempts);
            }
        } else {
            throw ex;
        }
        return attempts;
    }


    private int handleRetry(RuntimeException ex, int attempts) {
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
        return attempts;
    }

}
