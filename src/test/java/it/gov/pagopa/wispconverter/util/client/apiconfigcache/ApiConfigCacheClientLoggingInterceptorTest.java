package it.gov.pagopa.wispconverter.util.client.apiconfigcache;

import static org.junit.jupiter.api.Assertions.*;

import it.gov.pagopa.wispconverter.repository.model.enumz.WorkflowStatus;
import org.junit.jupiter.api.Test;

class ApiConfigCacheClientLoggingInterceptorTest {

  @Test
  void getOperationStatus() {
    var interceptor = new ApiConfigCacheClientLoggingInterceptor(null, null);
    var result = interceptor.getOperationStatus(null, null);
    assertEquals(WorkflowStatus.COMMUNICATION_WITH_APICONFIG_CACHE_PROCESSED, result);
  }
}
