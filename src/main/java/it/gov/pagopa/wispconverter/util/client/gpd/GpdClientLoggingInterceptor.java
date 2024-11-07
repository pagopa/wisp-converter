package it.gov.pagopa.wispconverter.util.client.gpd;

import it.gov.pagopa.wispconverter.repository.model.enumz.InternalStepStatus;
import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.client.AbstractAppClientLoggingInterceptor;
import it.gov.pagopa.wispconverter.util.client.ClientServiceEnum;
import it.gov.pagopa.wispconverter.util.client.RequestResponseLoggingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

import java.util.regex.Pattern;

@Slf4j
public class GpdClientLoggingInterceptor extends AbstractAppClientLoggingInterceptor {

    private final Pattern getDebtPositionPattern;

    public GpdClientLoggingInterceptor(RequestResponseLoggingProperties clientLoggingProperties, ReService reService, Boolean isTracingOfClientOnREEnabled) {
        super(clientLoggingProperties, reService, ClientServiceEnum.GPD);

        // avoiding persistence of client invocation on RE
        if (Boolean.FALSE.equals(isTracingOfClientOnREEnabled)) {
            avoidEventPersistenceOnRE();
        }

        // defining pattern for retrieve operational status
        getDebtPositionPattern = Pattern.compile("organizations/.+/paymentoptions/.+/debtposition");
    }

    @Override
    protected InternalStepStatus getOperationStatus(String url, HttpMethod httpMethod) {
        InternalStepStatus status;
        if (httpMethod.equals(HttpMethod.GET) && getDebtPositionPattern.asMatchPredicate().test(url)) {
            status = InternalStepStatus.COMMUNICATION_WITH_GPD_FOR_DEBT_POSITION_RETRIEVE_PROCESSED;
        } else {
            status = InternalStepStatus.COMMUNICATION_WITH_GPD_FOR_DEBT_POSITION_UPSERT_PROCESSED;
        }
        return status;
    }
}
