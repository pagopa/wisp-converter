package it.gov.pagopa.wispconverter.util.client;

import it.gov.pagopa.wispconverter.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class MDCInterceptor implements ClientHttpRequestInterceptor {


  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

      ClientHttpResponse response = execution.execute(request, body);

      MDC.remove(Constants.MDC_CLIENT_OPERATION_ID);
      MDC.remove(Constants.MDC_CLIENT_EXECUTION_TIME);

    return response;
  }
}
