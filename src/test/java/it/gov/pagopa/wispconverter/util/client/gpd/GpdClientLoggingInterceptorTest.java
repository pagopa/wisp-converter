package it.gov.pagopa.wispconverter.util.client.gpd;

import it.gov.pagopa.wispconverter.repository.model.enumz.WorkflowStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GpdClientLoggingInterceptorTest {

  @Test
  void getOperationStatus() {
    var interceptor = new GpdClientLoggingInterceptor(null, null, null);
    var result = interceptor.getOperationStatus("organizations", HttpMethod.GET);
    assertEquals(WorkflowStatus.COMMUNICATION_WITH_GPD_FOR_DEBT_POSITION_RETRIEVE_PROCESSED, result);
  }
}
