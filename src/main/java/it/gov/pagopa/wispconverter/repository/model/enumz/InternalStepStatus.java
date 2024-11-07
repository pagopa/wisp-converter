package it.gov.pagopa.wispconverter.repository.model.enumz;

public enum InternalStepStatus {

    RPTS_EXTRACTED,
    FOUND_RT_IN_STORAGE,
    CONVERSION_ERROR_SENDING_RT,
    GENERATING_RT_FOR_GPD_EXCEPTION,
    RT_NOT_GENERABLE_FOR_GPD_STATION,
    COMMUNICATION_WITH_CHECKOUT_FOR_CART_CREATION_PROCESSED,
    POSITIVE_RT_TRY_TO_SEND_TO_CREDITOR_INSTITUTION,
    CONVERSION_PROCESSED,
    RT_SEND_SUCCESS,
    RT_SENT_OK,
    RT_SEND_FAILURE,
    RT_ALREADY_SENT,
    RT_SEND_SCHEDULING_SUCCESS,
    RT_SEND_SCHEDULING_FAILURE,
    RT_SCHEDULED_SEND_SUCCESS,
    RT_SCHEDULED_SEND_FAILURE,
    RT_SEND_RESCHEDULING_FAILURE,
    RT_SEND_RESCHEDULING_REACHED_MAX_RETRIES,
    RT_SEND_RESCHEDULING_SUCCESS,
    RT_START_RECONCILIATION_PROCESS,
    RT_END_RECONCILIATION_PROCESS,
    RT_DEAD_LETTER_SAVED,
    RT_DEAD_LETTER_FAILED,
    PAYMENT_TOKEN_TIMER_CREATED,
    PAYMENT_TOKEN_TIMER_CREATION_PROCESSED,
    PAYMENT_TOKEN_TIMER_DELETED_SCHEDULING,
    PAYMENT_TOKEN_TIMER_DELETION_PROCESSED,
    PAYMENT_TOKEN_TIMER_DELETED,
    PAYMENT_TOKEN_TIMER_IN_TIMEOUT,
    ECOMMERCE_HANG_TIMER_TRIGGER,
    ECOMMERCE_HANG_TIMER_CREATED,
    RPT_TIMER_IN_TIMEOUT,
    RPT_TIMER_CREATED,
    RPT_TIMER_CREATION_FAILED,
    COMMUNICATION_WITH_GPD_FOR_DEBT_POSITION_RETRIEVE_PROCESSED,
    COMMUNICATION_WITH_IUVGENERATOR_FOR_NAV_CREATION_PROCESSED,
    COMMUNICATION_WITH_GPD_FOR_DEBT_POSITION_UPSERT_PROCESSED,
    COMMUNICATION_WITH_APIM_FOR_CACHING_RPT_MAPPING_PROCESSED,
    COMMUNICATION_WITH_APIM_FOR_CACHING_SESSION_MAPPING_PROCESSED,
    COMMUNICATING_WITH_CREDITOR_INSTITUTION_REQUEST,
    COMMUNICATING_WITH_CREDITOR_INSTITUTION_RESPONSE,
    ;

    public static InternalStepStatus getStatusFromClientCommunication(ClientEnum client, EventSubcategoryEnum point) {
        InternalStepStatus status = null;
        switch (client) {
            case GPD -> {
                    status = COMMUNICATION_WITH_GPD_FOR_DEBT_POSITION_RETRIEVE_PROCESSED;
            }
            case CHECKOUT -> {
                    status = COMMUNICATION_WITH_CHECKOUT_FOR_CART_CREATION_PROCESSED;

            }
            case IUV_GENERATOR -> {
                    status = COMMUNICATION_WITH_IUVGENERATOR_FOR_NAV_CREATION_PROCESSED;
            }
            case DECOUPLER_CACHING -> {
                status = COMMUNICATION_WITH_APIM_FOR_CACHING_RPT_MAPPING_PROCESSED;
            }
            case CREDITOR_INSTITUTION_ENDPOINT -> {
                if (EventSubcategoryEnum.REQ.equals(point)) {
                    status = COMMUNICATING_WITH_CREDITOR_INSTITUTION_REQUEST;
                } else if (EventSubcategoryEnum.RESP.equals(point)) {
                    status = COMMUNICATING_WITH_CREDITOR_INSTITUTION_RESPONSE;
                }
            }
        }
        return status;
    }
}
