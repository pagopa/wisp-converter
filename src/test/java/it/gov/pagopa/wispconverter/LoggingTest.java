package it.gov.pagopa.wispconverter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.client.RequestResponseLoggingProperties;
import it.gov.pagopa.wispconverter.util.client.apiconfigcache.ApiConfigCacheClientLoggingInterceptor;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

public class LoggingTest {


    @SneakyThrows
    @Test
    public void testClientLogger(){

        ReService reService = mock(ReService.class);
        HttpRequest httpRequest = mock(HttpRequest.class);
        ClientHttpResponse clientHttpResponse = mock(ClientHttpResponse.class);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
        when(httpRequest.getURI()).thenReturn(URI.create("http://localhost"));
        when(httpRequest.getHeaders()).thenReturn(new HttpHeaders());

        when(execution.execute(any(),any())).thenReturn(clientHttpResponse);
        when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(clientHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(clientHttpResponse.getBody()).thenReturn(new ByteArrayInputStream("payload".getBytes(StandardCharsets.UTF_8)));

        RequestResponseLoggingProperties requestResponseLoggingProperties = new RequestResponseLoggingProperties();
        requestResponseLoggingProperties.setRequest(
                new RequestResponseLoggingProperties.Request(null,true,true,1000,true)
        );
        requestResponseLoggingProperties.setResponse(
                new RequestResponseLoggingProperties.Response(true,true,1000,true)
        );

        ApiConfigCacheClientLoggingInterceptor interceptor = new ApiConfigCacheClientLoggingInterceptor(requestResponseLoggingProperties,reService);
        interceptor.intercept(httpRequest,"payload".getBytes(),execution);

    }
}
