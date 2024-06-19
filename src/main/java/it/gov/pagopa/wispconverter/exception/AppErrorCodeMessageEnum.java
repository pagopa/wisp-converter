package it.gov.pagopa.wispconverter.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum AppErrorCodeMessageEnum {
    ERROR(500, "System error", "{0}", HttpStatus.INTERNAL_SERVER_ERROR, "A not documented generic error occurred while execution. For better understanding the cause, please use the Technical Support's APIs."),
    // --- Internal logic errors ---
    GENERIC_ERROR(1000, "Generic flow error", "Error while executing conversion flow. {0}", HttpStatus.INTERNAL_SERVER_ERROR, "A generic error occurred while executing conversion flow. For better understanding the cause, please use the Technical Support's APIs."),
    // --- Parsing errors ---
    PARSING_GENERIC_ERROR(1001, "Generic parsing error", "Error while parsing payload. {0}", HttpStatus.BAD_REQUEST, "A generic error occurred while parsing of the content associated to the SOAP request related to nodoInviaRPT or nodoInviaCarrelloRPT. For better understanding the cause, please use the Technical Support's APIs."),
    PARSING_INVALID_HEADER(1002, "SOAP Header parsing error", "Error while parsing payload. The SOAP header in payload is invalid: {0}", HttpStatus.BAD_REQUEST, "An error occurred while parsing of the content header, associated to the SOAP request related to nodoInviaRPT or nodoInviaCarrelloRPT."),
    PARSING_INVALID_BODY(1003, "SOAP Body parsing error", "Error while parsing payload. The SOAP body in payload is invalid: {0}", HttpStatus.BAD_REQUEST, "An error occurred while parsing of the content payload, associated to the SOAP request related to nodoInviaRPT or nodoInviaCarrelloRPT."),
    PARSING_INVALID_XML_NODES(1004, "XML parsing error", "Error while parsing payload. The list of nodes extracted from document must be greater than zero, but currently it is zero.", HttpStatus.BAD_REQUEST, "An error occurred while parsing of the of the content associated to the SOAP request related to nodoInviaRPT or nodoInviaCarrelloRPT. The XML content extracted, either from payload or from header, is invalid because it is not possible to extract tag nodes from document. So, the document is probably empty."),
    PARSING_INVALID_ZIPPED_PAYLOAD(1005, "ZIP extraction error", "Error while parsing payload. Cannot unzip payload correctly.", HttpStatus.BAD_REQUEST, "An error occurred while parsing of the content associated to the SOAP request related to nodoInviaRPT or nodoInviaCarrelloRPT. The SOAP request analyzed and stored in dedicated storage is not usable for convert the debt positions in GPD system. This is probably due to an invalid conversion of the SOAP request via GZip algorithm executed before the same is stored in its storage."),
    PARSING_RPT_PRIMITIVE_NOT_VALID(1006, "Primitive not valid", "Error while checking primitive. Primitive [{0}] not valid.", HttpStatus.BAD_REQUEST, "An error occurred while parsing of the content associated to the SOAP request related to nodoInviaRPT or nodoInviaCarrelloRPT. The primitive (the content related to header 'soapaction') cannot be handled by WISP Converter application in redirect process: only one of nodoInviaRPT and nodoInviaCarrelloRPT can be accepted."),
    // --- Validation errors ---
    VALIDATION_INVALID_MULTIBENEFICIARY_CART(1100, "RPTs not valid", "Error while generating payment position for GPD service. The cart is defined as multi-beneficiary but there are a number of RPTs lower than 2.", HttpStatus.BAD_REQUEST, "An error occurred while analyzing the RPTs extracted from SOAP request. In particular, the request is arrived as nodoInviaCarrelloRPT as multi-beneficiary cart, but the number of RPTs in the request is lower than two, so it cannot be correctly handled as multi-beneficiary."),
    VALIDATION_INVALID_IBANS(1101, "IBANs not valid", "Error while generating payment position for GPD service. The IBAN field must be set if digital stamp is not defined for the transfer.", HttpStatus.BAD_REQUEST, "An error occurred while analyzing the RPTs extracted from SOAP request. An IBAN must always be set in RPT transfers if they aren't related to digital stamps (which don't require an IBAN, because they will be reported to specific subject). In this case, in one or more RPT transfers not related to digital stamp, the IBAN is not correctly set."),
    VALIDATION_INVALID_DEBTOR(1102, "Debtor subject not valid", "Error while generating payment position for GPD service. The debtor subject information is different between the various RPT of the cart.", HttpStatus.BAD_REQUEST, "An error occurred while analyzing the RPTs extracted from SOAP request. In particular, in a cart there are different debtor subjects and this is not permitted for this flow. So, the whole cart is discarded."),
    VALIDATION_INVALID_CREDITOR_INSTITUTION(1103, "Creditor institution not valid", "Error while generating payment position for GPD service. The creditor institution information is different between the various RPT of the cart.", HttpStatus.BAD_REQUEST, "An error occurred while analyzing the RPTs extracted from SOAP request. In particular, in a cart there are different RPT that refers to different payees, but this is not permitted when the cart is not for multi-beneficiary. So, the whole cart is discarded."),
    // --- Cache configuration errors ---
    CONFIGURATION_INVALID_CACHE(1200, "Cache not valid", "Error while retrieving data from cache configuration. No valid cached configuration found.", HttpStatus.NOT_FOUND, "An error occurred while trying to access data from cached configuration. It is possible that the cache is not retrieved yet by this service or a corrupted configuration was returned from APIConfig Cache internal service. If this problem still occurs, please check the connectivity with APIConfig Cache."),
    CONFIGURATION_INVALID_STATION(1201, "Station not valid", "Error while retrieving data from cached configuration. No valid station found with code [{0}].", HttpStatus.NOT_FOUND, "An error occurred while retrieving data from local cached configuration. In particular, it is not possible to retrieve the configuration about the station because it does not exists in cache, and maybe also in general configuration. So, a change in whole configuration and/or a refresh on cache is required."),
    CONFIGURATION_INVALID_CREDITOR_INSTITUTION_STATION(1202, "Creditor institution's station not valid", "Error while retrieving data from cached configuration. No valid station with segregation code [{0}] found in creditor institution with code [{1}].", HttpStatus.NOT_FOUND, "An error occurred while checking the station that will be used for the payment process. In particular, analyzing the station that is related to the segregation code extracted from a payment option's notice number, it turns out that the required station does not exists in cached configuration. So, a change in whole configuration and/or a refresh on cache is required."),
    CONFIGURATION_INVALID_STATION_REDIRECT_URL(1203, "Station not correctly configured", "Error while retrieving data from cached configuration. The station with code [{0}] contains wrong redirect URL and it is not possible to generate valid URI.", HttpStatus.UNPROCESSABLE_ENTITY, "An error occurred while checking the station that will be used for the payment process. In particular, analyzing the station that is related to the segregation code extracted from a payment option's notice number, it turns out that the configuration about redirection in error cases is not correctly set to points towards some creditor institution's endpoint. So, a change in configuration is required."),
    CONFIGURATION_INVALID_STATION_SERVICE_URL(1204, "Station not correctly configured", "Error while retrieving data from cached configuration. The station with code [{0}] contains wrong on missing service URL configuration and it is not possible to generate valid URI.", HttpStatus.UNPROCESSABLE_ENTITY, "An error occurred while checking the station that will be used for the payment process. In particular, analyzing the station that is related to the segregation code extracted from a payment option's notice number, it turns out that the configuration is not correctly set to points towards GPD service endpoint for RT generator. So, a change in configuration is required."),
    CONFIGURATION_INVALID_GPD_STATION(1205, "Station not correctly configured for GPD", "Error while retrieving data from cached configuration. The station with code [{0}] is onboarded on GPD but the primitive version is set to 1.", HttpStatus.UNPROCESSABLE_ENTITY, "An error occurred while checking the station that will be used for the payment process. In particular, analyzing the station that is related to the segregation code extracted from a payment option's notice number, it turns out that the configuration is correctly set to points towards GPD service endpoint but uses the 'v1' primitive version (and it must use the 'v2' version). So, a change in configuration is required."),
    // --- Payment position conversion errors ---
    PAYMENT_POSITION_NOT_IN_PAYABLE_STATE(1300, "Existing payment position not in payable state", "Error while generating payment position. One or more of the payment position(s) associated to IUV(s) [{0}] exists but are not in a payable state.", HttpStatus.UNPROCESSABLE_ENTITY, "An error occurred while checking an existing payment position. One or more RPTs extracted from the request refers to existing payment positions in GPD that have a state from which it is impossible to execute a payment flow. If the execution of this flow is related to a RPT cart, all the payments that can be retrieved or generated ex novo from those RPTs are declared as atomically invalid (if one RPT in cart is bad, all RPTs in cart are bad) and not payable with this flow."),
    PAYMENT_POSITION_IN_INCONSISTENT_STATE(1301, "Existing payment position in not consistent state", "Error while generating payment position. There is a payment position associated to IUV [{0}] but it is in an inconsistent state.", HttpStatus.UNPROCESSABLE_ENTITY, "An error occurred while checking an existing payment position in GPD system. The retrieved payment position, previously inserted in GPD by this same flow or by other procedures, is in an invalid state, not mappable to an existing value. This can be related to a wrong setting in GPD or a corruption of the retrieved data."),
    PAYMENT_POSITION_NOT_VALID(1302, "Existing payment position not valid", "Error while generating payment position. The payment position associated to IUV [{0}] is not valid. {1}", HttpStatus.UNPROCESSABLE_ENTITY, "An error occurred while generating a payment position. In particular, something during the generation of a new payment position or the analysis of an existing payment position went wrong and the operation cannot be completed.  For better understanding the cause, please use the Technical Support's APIs."),
    PAYMENT_OPTION_NOT_EXTRACTABLE(1303, "Impossible to extract payment option", "Error while extracting payment option. An error occurred while trying to extract payment option.", HttpStatus.UNPROCESSABLE_ENTITY, "An error occurred while extracting a payment option from a payment position. This can be caused by a malformed payment position that does not have a payment option. Remember that a payment position in this flow must have one and only one payment option."),
    // --- Receipt generation errors ---
    RECEIPT_KO_NOT_GENERATED(1400, "KO Receipt not generated", "Error while generating KO receipt. It is not possible to generate the receipt due to an error: [{0}].", HttpStatus.BAD_REQUEST, "An error occurred while generating a negative RT (aka a KO receipt). So, no receipt can be sent lately to creditor institution and probably the process must be executed manually. For better understanding the cause, please use the Technical Support's APIs."),
    // --- DB and storage interaction errors ---
    PERSISTENCE_SAVING_RE_ERROR(2000, "Impossible to save event", "Error while trying to store an event in Registro Eventi. Impossible to store event: {0}.", HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred wile trying to store a new event in the Registro Eventi storage. The error is somewhat related to a persistence problem of the used storage and in the majority of the cases is temporary (maybe a 429 HTTP code). This error currently blocks the entire flow because that can lead to untraceable requests. For better understanding the cause, please execute a search in the log provider (Application Insights, Kibana, etc)."),
    PERSISTENCE_RPT_NOT_FOUND(2001, "RPT not found", "Error while retrieving RPT. RPT with sessionId [{0}] not found.", HttpStatus.NOT_FOUND, "An error occurred while trying to retrieve the RPT content saved in storage by WISP SOAP Converter. This can be related either with the use of a wrong sessionId or a missed persistence from WISP SOAP Converter, so it is better to analyze the entire flow using Technical Support's APIs. This block totally the conversion of the RPTs in GPD's payment positions, so the whole process is discarded."),
    PERSISTENCE_REQUESTID_CACHING_ERROR(2002, "RequestID caching error", "Error while reading cached RequestID. No valid value found for key [{0}].", HttpStatus.NOT_FOUND, "An error occurred while trying to retrieve data from internal cache. Specifically, the cached key, defined in format wisp_nav2iuv_<domainId>_<nav> needed for RT generation, was not found. This missing read invalidates the entire process of the RT generation."),
    PERSISTENCE_MAPPING_NAV_TO_IUV_ERROR(2003, "Mapping caching error", "Error while reading cached mapping from NAV to IUV. No valid value found for NAV-based key [{0}].", HttpStatus.NOT_FOUND, "An error occurred while trying to retrieve data from internal cache. Specifically, the cached key, defined in format wisp_nav2iuv_<domainId>_<nav> needed for RT generation is corrupted in some way, because it is not possible to extract 'domainId' or 'nav' correctly. This wrong read invalidates the entire process of the RT generation."),
    // --- Client errors ---
    CLIENT_APICONFIGCACHE(3000, "APIConfig cache client error", "Error while communicating with APIConfig cache service. {0}", HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while communicating with APIConfig Cache backend internal service in order to retrieve the last generated cache. It can be related to any client problem, so the best way to handle this is to use the Technical Support's APIs in order to find the cause."),
    CLIENT_GPD(3001, "GPD client error", "Error while communicating with GPD service. {0}", HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while communicating with GPD backend services in order to execute a specific operation on payment position. It can be related to any client problem and to any particular operation, so the best way to handle this is to use the Technical Support's APIs in order to find the cause."),
    CLIENT_IUVGENERATOR(3002, "IUV Generator client error", "Error while communicating with IUV Generator service. {0}", HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while communicating with IUV Generator backend internal service in order to retrieve a newly generated NAV code to be used to the new payment position to send in GPD system. It can be related to any client problem, so the best way to handle this is to use the Technical Support's APIs in order to find the cause."),
    CLIENT_DECOUPLER_CACHING(3003, "Decoupler caching client error", "Error while communicating with decoupler caching API. {0}", HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while communicating with an internal service endpoint dedicated to storing internal cache for route requests in Decoupler. It can be related to any client problem, so the best way to handle this is to use the Technical Support's APIs in order to find the cause."),
    CLIENT_CHECKOUT(3004, "Checkout error", "Error while communicating with Checkout service. {0}", HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while communicating with Checkout backend internal service in order to send a request about the cart creation. It can be related to any client problem, so the best way to handle this is to use the Technical Support's APIs in order to find the cause."),
    CLIENT_PAAINVIART(3005, "PaaInviaRT error", "Error while communicating with Station for paaInviaRT service. {0}", HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while communicating with the creditor institution's station (external service) in order to sending of a paaInviaRT request. It can be related to any client problem, so the best way to handle this is to use the Technical Support's APIs in order to find the cause."),
    SERVICE_BUS_CLIENT_CANCEL_ERROR(3006, "ServiceBus Error", "Error while communicating with Service Bus for cancel scheduled message related to payment token timeout. {0}", HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while communicating with Service Bus for cancel scheduled message related to payment token"),
    ;

    private final Integer code;
    private final String title;
    private final String detail;
    private final HttpStatusCode status;
    private final String openapiDescription;

    AppErrorCodeMessageEnum(Integer code, String title, String detail, HttpStatus status, String openapiDescription) {
        this.code = code;
        this.title = title;
        this.detail = detail;
        this.status = status;
        this.openapiDescription = openapiDescription;
    }
}
