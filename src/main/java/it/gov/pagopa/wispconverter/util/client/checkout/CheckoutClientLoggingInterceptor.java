package it.gov.pagopa.wispconverter.util.client.checkout;

import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.client.AbstractAppClientLoggingInterceptor;
import it.gov.pagopa.wispconverter.util.client.ClientServiceEnum;
import it.gov.pagopa.wispconverter.util.client.RequestResponseLoggingProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

@Slf4j
public class CheckoutClientLoggingInterceptor extends AbstractAppClientLoggingInterceptor {

  public CheckoutClientLoggingInterceptor(RequestResponseLoggingProperties clientLoggingProperties, ReService reService){
    super(clientLoggingProperties, reService, ClientServiceEnum.CHECKOUT);
  }
  @Override
  protected void request(String clientOperationId, String operationId, HttpRequest request, byte[] reqBody) {
    if(log.isDebugEnabled()){
      log.debug(createRequestMessage(clientOperationId, operationId, request, reqBody));
    }
  }

  @SneakyThrows
  @Override
  protected void response(String clientOperationId, String operationId, String clientExecutionTime, HttpRequest request, ClientHttpResponse response) {
    if(log.isDebugEnabled()){
      log.debug(createResponseMessage(clientOperationId, operationId, clientExecutionTime, request, response));
    }
  }
}
