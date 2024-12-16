package it.gov.pagopa.wispconverter.util.client.gpd;

import it.gov.pagopa.wispconverter.repository.model.enumz.WorkflowStatus;
import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.client.AbstractAppClientLoggingInterceptor;
import it.gov.pagopa.wispconverter.util.client.ClientServiceEnum;
import it.gov.pagopa.wispconverter.util.client.RequestResponseLoggingProperties;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class GpdClientLoggingInterceptor extends AbstractAppClientLoggingInterceptor {

    private final Pattern navPattern = Pattern.compile("\\\\\"nav\\\\\":\\\\\"([0-9]+)\\\\\"");


    public GpdClientLoggingInterceptor(RequestResponseLoggingProperties clientLoggingProperties, ReService reService, Boolean isTracingOfClientOnREEnabled) {
        super(clientLoggingProperties, reService, ClientServiceEnum.GPD);

        // avoiding persistence of client invocation on RE
        if (Boolean.FALSE.equals(isTracingOfClientOnREEnabled)) {
            avoidEventPersistenceOnRE();
        }

    }

    @Override
    protected WorkflowStatus getOperationStatus(String url, HttpMethod httpMethod) {
        WorkflowStatus status;
        if (httpMethod.equals(HttpMethod.GET)) {
            status = WorkflowStatus.COMMUNICATION_WITH_GPD_FOR_DEBT_POSITION_RETRIEVE_PROCESSED;
        } else {
            status = WorkflowStatus.COMMUNICATION_WITH_GPD_FOR_DEBT_POSITION_UPSERT_PROCESSED;
        }
        return status;
    }

    @Override
    protected void setInMDCAfterClientResponse(String payload) {
        try {
            if (payload != null && !payload.isBlank()) {
                Matcher matcher = navPattern.matcher(payload);
                if (matcher.find()) {
                    String noticeNumber = matcher.group(0);
                    if (noticeNumber != null && !noticeNumber.isBlank()) {
                        MDC.put(Constants.MDC_NOTICE_NUMBER, noticeNumber);
                    }
                }
            }
        } catch (Exception e) {
            // catch this but do nothing, as nothing is required to do
        }
    }

    @Override
    protected void deleteFromMDCAfterClientResponse() {
        MDC.remove(Constants.MDC_NOTICE_NUMBER);
    }
}
