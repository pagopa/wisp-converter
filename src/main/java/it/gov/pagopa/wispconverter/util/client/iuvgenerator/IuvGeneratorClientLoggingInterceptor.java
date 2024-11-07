package it.gov.pagopa.wispconverter.util.client.iuvgenerator;

import it.gov.pagopa.wispconverter.repository.model.enumz.InternalStepStatus;
import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.client.AbstractAppClientLoggingInterceptor;
import it.gov.pagopa.wispconverter.util.client.ClientServiceEnum;
import it.gov.pagopa.wispconverter.util.client.RequestResponseLoggingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

@Slf4j
public class IuvGeneratorClientLoggingInterceptor extends AbstractAppClientLoggingInterceptor {

    public IuvGeneratorClientLoggingInterceptor(RequestResponseLoggingProperties clientLoggingProperties, ReService reService, Boolean isTracingOfClientOnREEnabled) {
        super(clientLoggingProperties, reService, ClientServiceEnum.IUV_GENERATOR);

        // avoiding persistence of client invocation on RE
        if (Boolean.FALSE.equals(isTracingOfClientOnREEnabled)) {
            avoidEventPersistenceOnRE();
        }
    }

    @Override
    protected InternalStepStatus getOperationStatus(String url, HttpMethod httpMethod) {
        return InternalStepStatus.COMMUNICATION_WITH_IUVGENERATOR_FOR_NAV_CREATION_PROCESSED;
    }
}
