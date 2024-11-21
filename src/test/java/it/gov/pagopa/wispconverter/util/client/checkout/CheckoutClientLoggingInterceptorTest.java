package it.gov.pagopa.wispconverter.util.client.checkout;

import static org.junit.jupiter.api.Assertions.*;

import it.gov.pagopa.wispconverter.repository.model.enumz.WorkflowStatus;
import org.junit.jupiter.api.Test;

class CheckoutClientLoggingInterceptorTest {

  @Test
  void getOperationStatus() {
    var interceptor = new CheckoutClientLoggingInterceptor(null, null, null);
    var result = interceptor.getOperationStatus(null, null);
    assertEquals(WorkflowStatus.COMMUNICATION_WITH_CHECKOUT_FOR_CART_CREATION_PROCESSED, result);
  }
}
