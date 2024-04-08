package it.gov.pagopa.wispconverter.util.client;

import it.gov.pagopa.wispconverter.service.ReService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class ReInterceptor implements ClientHttpRequestInterceptor {


  private final ReService reService;

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

//      MDC.getCopyOfContextMap().forEach((k, v) -> {
//        log.debug(String.format("BEFORE MDC %s=%s",k, v));
//      });
      reService.addRe("CLIENT IN");
      ClientHttpResponse response = execution.execute(request, body);

//      MDC.getCopyOfContextMap().forEach((k,v) -> {
//        log.debug(String.format("AFTER MDC %s=%s",k, v));
//      });
      reService.addRe("CLIENT OUT");

    return response;
  }
}
