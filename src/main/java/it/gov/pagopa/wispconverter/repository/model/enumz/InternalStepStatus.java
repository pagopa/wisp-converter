package it.gov.pagopa.wispconverter.repository.model.enumz;

public enum InternalStepStatus {

    FOUND_RPT_IN_STORAGE,
    FOUND_RT_IN_STORAGE,
    EXTRACTED_DATA_FROM_RPT,
    CREATED_NEW_PAYMENT_POSITION_IN_GPD,
    GENERATED_NAV_FOR_NEW_PAYMENT_POSITION,
    UPDATED_EXISTING_PAYMENT_POSITION_IN_GPD,
    FOUND_INVALID_PAYMENT_POSITION_IN_GPD,
    GENERATED_CACHE_ABOUT_RPT_FOR_DECOUPLER,
    GENERATED_CACHE_ABOUT_RPT_FOR_RT_GENERATION,
    SAVED_RPT_IN_CART_RECEIVED_REDIRECT_URL_FROM_CHECKOUT,
    GENERATED_NEW_RT,
    NEGATIVE_RT_NOT_GENERABLE_FOR_GPD_STATION,
    NEGATIVE_RT_GENERATION_SKIPPED,
    NEGATIVE_RT_SENDING_TO_CREDITOR_INSTITUTION,
    POSITIVE_RT_SENDING_TO_CREDITOR_INSTITUTION,
    NEGATIVE_RT_GENERATION_SUCCESS,
    POSITIVE_RT_GENERATION_SUCCESS,
    RT_SEND_SUCCESS,
    RT_SEND_FAILURE,
    RT_SEND_SCHEDULING_SUCCESS,
    RT_SEND_SCHEDULING_FAILURE,
    RT_SCHEDULED_SEND_SUCCESS,
    RT_SCHEDULED_SEND_FAILURE,
    RT_SEND_RESCHEDULING_FAILURE,
    RT_SEND_RESCHEDULING_SUCCESS,

    COMMUNICATING_WITH_GPD_REQUEST,
    COMMUNICATING_WITH_GPD_RESPONSE,

    COMMUNICATING_WITH_IUV_GENERATOR_REQUEST,
    COMMUNICATING_WITH_IUV_GENERATOR_RESPONSE,

    COMMUNICATING_WITH_CHECKOUT_REQUEST,
    COMMUNICATING_WITH_CHECKOUT_RESPONSE,

    COMMUNICATING_WITH_DECOUPLER_CACHING_REQUEST,
    COMMUNICATING_WITH_DECOUPLER_CACHING_RESPONSE,
    ;

    public static InternalStepStatus getStatusFromClientCommunication(ClientEnum client, EventSubcategoryEnum point) {
        InternalStepStatus status = null;
        switch (client) {
            case GPD -> {
                if (EventSubcategoryEnum.REQ.equals(point)) {
                    status = COMMUNICATING_WITH_GPD_REQUEST;
                } else if (EventSubcategoryEnum.RESP.equals(point)) {
                    status = COMMUNICATING_WITH_GPD_RESPONSE;
                }
            }
            case CHECKOUT -> {
                if (EventSubcategoryEnum.REQ.equals(point)) {
                    status = COMMUNICATING_WITH_CHECKOUT_REQUEST;
                } else if (EventSubcategoryEnum.RESP.equals(point)) {
                    status = COMMUNICATING_WITH_CHECKOUT_RESPONSE;
                }
            }
            case IUV_GENERATOR -> {
                if (EventSubcategoryEnum.REQ.equals(point)) {
                    status = COMMUNICATING_WITH_IUV_GENERATOR_REQUEST;
                } else if (EventSubcategoryEnum.RESP.equals(point)) {
                    status = COMMUNICATING_WITH_IUV_GENERATOR_RESPONSE;
                }
            }
            case DECOUPLER_CACHING -> {
                if (EventSubcategoryEnum.REQ.equals(point)) {
                    status = COMMUNICATING_WITH_DECOUPLER_CACHING_REQUEST;
                } else if (EventSubcategoryEnum.RESP.equals(point)) {
                    status = COMMUNICATING_WITH_DECOUPLER_CACHING_RESPONSE;
                }
            }
        }
        return status;
    }
}
