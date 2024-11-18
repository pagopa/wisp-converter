package it.gov.pagopa.wispconverter.util.client.checkout;

import it.gov.pagopa.wispconverter.repository.model.enumz.WorkflowStatus;
import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.client.AbstractAppClientLoggingInterceptor;
import it.gov.pagopa.wispconverter.util.client.ClientServiceEnum;
import it.gov.pagopa.wispconverter.util.client.RequestResponseLoggingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

@Slf4j
public class CheckoutClientLoggingInterceptor extends AbstractAppClientLoggingInterceptor {

    public CheckoutClientLoggingInterceptor(RequestResponseLoggingProperties clientLoggingProperties, ReService reService, Boolean isTracingOfClientOnREEnabled) {
        super(clientLoggingProperties, reService, ClientServiceEnum.CHECKOUT);

        // avoiding persistence of client invocation on RE
        if (Boolean.FALSE.equals(isTracingOfClientOnREEnabled)) {
            avoidEventPersistenceOnRE();
        }
    }

    @Override
    protected WorkflowStatus getOperationStatus(String url, HttpMethod httpMethod) {
        return WorkflowStatus.COMMUNICATION_WITH_CHECKOUT_FOR_CART_CREATION_PROCESSED;
    }
}
