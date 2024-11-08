package it.gov.pagopa.wispconverter.util.client.iuvgenerator;

import static org.junit.jupiter.api.Assertions.*;

import it.gov.pagopa.wispconverter.repository.model.enumz.WorkflowStatus;
import org.junit.jupiter.api.Test;

class IuvGeneratorClientLoggingInterceptorTest {

    @Test
    void getOperationStatus() {
        var interceptor = new IuvGeneratorClientLoggingInterceptor(null, null, null);
        var result = interceptor.getOperationStatus(null, null);
        assertEquals(WorkflowStatus.COMMUNICATION_WITH_IUVGENERATOR_FOR_NAV_CREATION_PROCESSED, result);
    }
}
