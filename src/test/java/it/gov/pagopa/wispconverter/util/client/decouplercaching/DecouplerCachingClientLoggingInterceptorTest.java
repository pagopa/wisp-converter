package it.gov.pagopa.wispconverter.util.client.decouplercaching;

import static org.junit.jupiter.api.Assertions.*;

import it.gov.pagopa.wispconverter.repository.model.enumz.WorkflowStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class DecouplerCachingClientLoggingInterceptorTest {

  @Test
  void getOperationStatus() {
    var interceptor = new DecouplerCachingClientLoggingInterceptor(null, null, null);
    var result = interceptor.getOperationStatus("/save-mapping", HttpMethod.GET);
    assertEquals(WorkflowStatus.COMMUNICATION_WITH_APIM_FOR_CACHING_RPT_MAPPING_PROCESSED, result);
  }
}
