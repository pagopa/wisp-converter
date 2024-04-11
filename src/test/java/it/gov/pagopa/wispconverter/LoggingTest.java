package it.gov.pagopa.wispconverter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.gov.pagopa.wispconverter.controller.ReceiptController;
import it.gov.pagopa.wispconverter.controller.model.ReceiptRequest;
import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.Trace;
import it.gov.pagopa.wispconverter.util.client.RequestResponseLoggingProperties;
import it.gov.pagopa.wispconverter.util.client.apiconfigcache.ApiConfigCacheClientLoggingInterceptor;
import it.gov.pagopa.wispconverter.util.interceptor.AppServerLoggingInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.method.HandlerMethod;

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

    @SneakyThrows
    @Test
    public void testServerLogger(){

        Method method = ReceiptController.class.getMethod("receiptOk", ReceiptRequest.class);
        HandlerMethod handlerMethod = new HandlerMethod(new ReceiptController(null),method);

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);

    //        when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
    //        when(httpRequest.getURI()).thenReturn(URI.create("http://localhost"));
    when(httpRequest.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList("h1")));
    when(httpRequest.getHeaders("h1")).thenReturn(Collections.enumeration(Arrays.asList("h1value")));
//
//        when(execution.execute(any(),any())).thenReturn(clientHttpResponse);
//        when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
//        when(clientHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
//        when(clientHttpResponse.getBody()).thenReturn(new ByteArrayInputStream("payload".getBytes(StandardCharsets.UTF_8)));

        RequestResponseLoggingProperties requestResponseLoggingProperties = new RequestResponseLoggingProperties();
        requestResponseLoggingProperties.setRequest(
                new RequestResponseLoggingProperties.Request(null,true,true,1000,true)
        );
        requestResponseLoggingProperties.setResponse(
                new RequestResponseLoggingProperties.Response(true,true,1000,true)
        );

        AppServerLoggingInterceptor interceptor = new AppServerLoggingInterceptor(requestResponseLoggingProperties);
        interceptor.preHandle(httpRequest,httpResponse,handlerMethod);
        interceptor.afterCompletion(httpRequest,httpResponse,handlerMethod,null);

    }
}
