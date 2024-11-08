package it.gov.pagopa.wispconverter.repository.model.enumz;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum WorkflowStatus {

    // === INTERNAL ===
    RPTS_EXTRACTED(EventCategoryEnum.INTERNAL, "Data extracted from all analyzed RPTs"),
    FOUND_RT_IN_STORAGE(EventCategoryEnum.INTERNAL, "RT found in storage"),
    GENERATING_RT_FOR_GPD_EXCEPTION(EventCategoryEnum.INTERNAL, "Error generating RT for GPD due to an exception"),
    RT_NOT_GENERABLE_FOR_GPD_STATION(EventCategoryEnum.INTERNAL, "RT cannot be generated for the GPD station"),
    POSITIVE_RT_TRY_TO_SEND_TO_CREDITOR_INSTITUTION(EventCategoryEnum.INTERNAL, "Successfully tried to send RT to creditor institution"),
    RT_SEND_SUCCESS(EventCategoryEnum.INTERNAL, "RT sent successfully"),
    RT_SENT_OK(EventCategoryEnum.INTERNAL, "RT sent successfully"),
    RT_SEND_FAILURE(EventCategoryEnum.INTERNAL, "Failure in sending RT"),
    RT_ALREADY_SENT(EventCategoryEnum.INTERNAL, "RT was already sent"),
    RT_SEND_SCHEDULING_SUCCESS(EventCategoryEnum.INTERNAL, "RT send scheduling succeeded"),
    RT_SEND_SCHEDULING_FAILURE(EventCategoryEnum.INTERNAL, "RT send scheduling failed"),
    RT_SCHEDULED_SEND_SUCCESS(EventCategoryEnum.INTERNAL, "RT scheduled send succeeded"),
    RT_SCHEDULED_SEND_FAILURE(EventCategoryEnum.INTERNAL, "RT scheduled send failed"),
    RT_SEND_RESCHEDULING_FAILURE(EventCategoryEnum.INTERNAL, "RT rescheduling failed"),
    RT_SEND_RESCHEDULING_REACHED_MAX_RETRIES(EventCategoryEnum.INTERNAL, "RT rescheduling reached maximum retries"),
    RT_SEND_RESCHEDULING_SUCCESS(EventCategoryEnum.INTERNAL, "RT rescheduling succeeded"),
    RT_START_RECONCILIATION_PROCESS(EventCategoryEnum.INTERNAL, "RT reconciliation process started"),
    RT_END_RECONCILIATION_PROCESS(EventCategoryEnum.INTERNAL, "RT reconciliation process ended"),
    RT_DEAD_LETTER_SAVED(EventCategoryEnum.INTERNAL, "RT dead letter saved"),
    RT_DEAD_LETTER_FAILED(EventCategoryEnum.INTERNAL, "RT dead letter failed"),
    PAYMENT_TOKEN_TIMER_CREATED(EventCategoryEnum.INTERNAL, "Payment token timeout timer created"),
    PAYMENT_TOKEN_TIMER_DELETED_SCHEDULING(EventCategoryEnum.INTERNAL, "Payment token timeout timer deleted from scheduling"),
    PAYMENT_TOKEN_TIMER_DELETED(EventCategoryEnum.INTERNAL, "Payment token timeout timer deleted"),
    ECOMMERCE_HANG_TIMER_TRIGGER(EventCategoryEnum.INTERNAL, "Ecommerce hang timer triggered"),
    ECOMMERCE_HANG_TIMER_CREATED(EventCategoryEnum.INTERNAL, "Ecommerce hang timer created"),
    RPT_TIMER_CREATED(EventCategoryEnum.INTERNAL, "RPT timer created"),

    // === CLIENT STATUS ===
    COMMUNICATION_WITH_GPD_FOR_DEBT_POSITION_RETRIEVE_PROCESSED(EventCategoryEnum.INTERNAL, "Request to gpd-core service to retrieve debt position"),
    COMMUNICATION_WITH_APICONFIG_CACHE_PROCESSED(EventCategoryEnum.INTERNAL, "Request to apiconfig cache service to retrieve the cache"),
    COMMUNICATION_WITH_IUVGENERATOR_FOR_NAV_CREATION_PROCESSED(EventCategoryEnum.INTERNAL, "Request to iuv-generator-core service for NAV creation"),
    COMMUNICATION_WITH_GPD_FOR_DEBT_POSITION_UPSERT_PROCESSED(EventCategoryEnum.INTERNAL, "Request to gpd-core service for debt position upsert"),
    COMMUNICATION_WITH_APIM_FOR_CACHING_RPT_MAPPING_PROCESSED(EventCategoryEnum.INTERNAL, "Request to APIM for caching RPT mapping"),
    COMMUNICATION_WITH_APIM_FOR_CACHING_SESSION_MAPPING_PROCESSED(EventCategoryEnum.INTERNAL, "Request to APIM for caching session mapping"),
    COMMUNICATION_WITH_CHECKOUT_FOR_CART_CREATION_PROCESSED(EventCategoryEnum.INTERNAL, "Request to checkout service for cart creation"),
    COMMUNICATION_WITH_CREDITOR_INSTITUTION_PROCESSED(EventCategoryEnum.INTERNAL, "Request to creditor institution processed"),

    // === INVOKED ENDPOINTS ===
    RPT_TIMER_CREATION_PROCESSED(EventCategoryEnum.INTERFACE, "Request to wisp-converter service for RPT timer creation"),
    RPT_TIMER_DELETION_PROCESSED(EventCategoryEnum.INTERFACE, "Request to wisp-converter service for RPT timer deletion"),
    CONVERSION_PROCESSED(EventCategoryEnum.INTERFACE, "Request to wisp-converter service for conversion"),
    CONVERSION_ERROR_SENDING_RT(EventCategoryEnum.INTERFACE, "Invalid Payment Position status"),
    PAYMENT_TOKEN_TIMER_CREATION_PROCESSED(EventCategoryEnum.INTERFACE, "Request to wisp-converter service for payment token timeout timer creation"),
    PAYMENT_TOKEN_TIMER_DELETION_PROCESSED(EventCategoryEnum.INTERFACE, "Request to wisp-converter service for payment token timeout timer deletion"),
    RECEIPT_SEND_PROCESSED(EventCategoryEnum.INTERFACE, "Receipt send processed"),

    // === TRIGGERED TIMER ===
    RPT_TIMER_IN_TIMEOUT(EventCategoryEnum.INTERFACE, "RPT timer triggered by timeout"),
    PAYMENT_TOKEN_TIMER_IN_TIMEOUT(EventCategoryEnum.INTERFACE, "Payment token timer triggered by timeout"),
    ECOMMERCE_HANG_TIMER_IN_TIMEOUT(EventCategoryEnum.INTERFACE, "Ecommerce hang timer triggered by timeout");

    private final EventCategoryEnum type;
    private final String description;


    public static WorkflowStatus findValue(String value) {
        return Arrays.stream(WorkflowStatus.values()).toList()
                .stream()
                .filter(elem -> elem.name().equals(value))
                .findFirst()
                .orElse(WorkflowStatus.RT_SENT_OK);
    }

}
