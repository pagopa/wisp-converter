package it.gov.pagopa.wispconverter.repository.model.enumz;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkflowStatus {

    // === INTERNAL ===
    RPTS_EXTRACTED(EventCategoryEnum.INTERNAL, "Data extracted from all analyzed RPTs"),
    FOUND_RT_IN_STORAGE(EventCategoryEnum.INTERNAL, "RT found in storage"),
    RPT_TIMER_DELETED(EventCategoryEnum.INTERNAL, "Timer RPT deleted from the cache"),
    ECOMMERCE_HANG_TIMER_CREATED(EventCategoryEnum.INTERNAL, "Ecommerce hang timer created"),
    ECOMMERCE_HANG_TIMER_DELETED(EventCategoryEnum.INTERNAL, "Ecommerce hang timer deleted"),
    CONVERSION_ERROR_SENDING_RT(EventCategoryEnum.INTERFACE, "Invalid Payment Position status"),
    RT_SEND_SUCCESS(EventCategoryEnum.INTERNAL, "RT sent successfully"),
    RT_SEND_FAILURE(EventCategoryEnum.INTERNAL, "Failure in sending RT"),
    RT_SEND_MOVED_IN_DEADLETTER(EventCategoryEnum.INTERNAL, "RT dead letter saved"),
    RT_SEND_SKIPPED_FOR_GPD_STATION(EventCategoryEnum.INTERNAL, "RT cannot be generated for the GPD station"),
    RT_SEND_SCHEDULING_SUCCESS(EventCategoryEnum.INTERNAL, "RT send scheduling succeeded"),
    RT_SEND_SCHEDULING_FAILURE(EventCategoryEnum.INTERNAL, "RT send scheduling failed"),
    RT_START_RECONCILIATION_PROCESS(EventCategoryEnum.INTERNAL, "RT reconciliation process started"),
    RT_END_RECONCILIATION_PROCESS(EventCategoryEnum.INTERNAL, "RT reconciliation process ended"),

    // === CLIENT STATUS ===
    COMMUNICATION_WITH_GPD_FOR_DEBT_POSITION_RETRIEVE_PROCESSED(EventCategoryEnum.INTERNAL, "Request to gpd-core service to retrieve debt position"),
    COMMUNICATION_WITH_APICONFIG_CACHE_PROCESSED(EventCategoryEnum.INTERNAL, "Request to apiconfig cache service to retrieve the cache"),
    COMMUNICATION_WITH_IUVGENERATOR_FOR_NAV_CREATION_PROCESSED(EventCategoryEnum.INTERNAL, "Request to iuv-generator-core service for NAV creation"),
    COMMUNICATION_WITH_GPD_FOR_DEBT_POSITION_UPSERT_PROCESSED(EventCategoryEnum.INTERNAL, "Request to gpd-core service for debt position upsert"),
    COMMUNICATION_WITH_APIM_FOR_CACHING_RPT_MAPPING_PROCESSED(EventCategoryEnum.INTERNAL, "Request to APIM for caching RPT mapping"),
    COMMUNICATION_WITH_APIM_FOR_CACHING_SESSION_MAPPING_PROCESSED(EventCategoryEnum.INTERNAL, "Request to APIM for caching session mapping"),
    COMMUNICATION_WITH_APIM_FOR_DELETING_SESSION_MAPPING_PROCESSED(EventCategoryEnum.INTERNAL, "Request to APIM for delete cached session mapping"),
    COMMUNICATION_WITH_CHECKOUT_FOR_CART_CREATION_PROCESSED(EventCategoryEnum.INTERNAL, "Request to checkout service for cart creation"),
    COMMUNICATION_WITH_CREDITOR_INSTITUTION_PROCESSED(EventCategoryEnum.INTERNAL, "Request to creditor institution processed"),

    // === INVOKED ENDPOINTS ===
    RPT_TIMER_CREATION_PROCESSED(EventCategoryEnum.INTERFACE, "Request to wisp-converter service for RPT timer creation"),
    RPT_TIMER_DELETION_PROCESSED(EventCategoryEnum.INTERFACE, "Request to wisp-converter service for RPT timer deletion"),
    CONVERSION_PROCESSED(EventCategoryEnum.INTERFACE, "Request to wisp-converter service for conversion"),
    PAYMENT_TOKEN_TIMER_CREATION_PROCESSED(EventCategoryEnum.INTERFACE, "Request to wisp-converter service for payment token timeout timer creation"),
    PAYMENT_TOKEN_TIMER_DELETION_PROCESSED(EventCategoryEnum.INTERFACE, "Request to wisp-converter service for payment token timeout timer deletion"),
    RECEIPT_SEND_PROCESSED(EventCategoryEnum.INTERFACE, "Receipt send processed"),

    // === TRIGGERED TIMER ===
    RECEIPT_RESEND_PROCESSED(EventCategoryEnum.INTERFACE, ""),
    RPT_TIMER_IN_TIMEOUT(EventCategoryEnum.INTERFACE, "RPT timer triggered by timeout"),
    ECOMMERCE_HANG_TIMER_IN_TIMEOUT(EventCategoryEnum.INTERFACE, "Ecommerce hang timer triggered by timeout"),
    PAYMENT_TOKEN_TIMER_IN_TIMEOUT(EventCategoryEnum.INTERFACE, "Payment token timer triggered by timeout");

    private final EventCategoryEnum type;
    private final String description;
}
