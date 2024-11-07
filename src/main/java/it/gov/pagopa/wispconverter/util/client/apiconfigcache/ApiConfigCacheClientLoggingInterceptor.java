package it.gov.pagopa.wispconverter.util.client.apiconfigcache;

import it.gov.pagopa.wispconverter.repository.model.enumz.InternalStepStatus;
import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.client.AbstractAppClientLoggingInterceptor;
import it.gov.pagopa.wispconverter.util.client.ClientServiceEnum;
import it.gov.pagopa.wispconverter.util.client.RequestResponseLoggingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

@Slf4j
public class ApiConfigCacheClientLoggingInterceptor extends AbstractAppClientLoggingInterceptor {

    public ApiConfigCacheClientLoggingInterceptor(RequestResponseLoggingProperties clientLoggingProperties, ReService reService) {
        super(clientLoggingProperties, reService, ClientServiceEnum.API_CONFIG_CACHE);

        // avoiding persistence of APIConfig Cache invocation on RE
        avoidEventPersistenceOnRE();
    }

    @Override
    protected InternalStepStatus getOperationStatus(String url, HttpMethod httpMethod) {
        return null;
    }
}
