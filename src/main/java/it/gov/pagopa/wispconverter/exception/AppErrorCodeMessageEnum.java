package it.gov.pagopa.wispconverter.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum AppErrorCodeMessageEnum {
    ERROR(500, "System error", "{0}", HttpStatus.INTERNAL_SERVER_ERROR, "A not documented generic error occurred during the execution. For better understanding the cause, please use the Technical Support's APIs."),
    // --- Internal logic errors ---
    GENERIC_ERROR(1000, "Generic flow error", "Error while executing conversion flow. {0}", HttpStatus.UNPROCESSABLE_ENTITY, "A generic error occurred during the conversion flow. For better understanding the cause, please use the Technical Support's APIs."),
    PARSING_GENERIC_ERROR(1001, "Generic parsing error", "Error while parsing payload. {0}", HttpStatus.BAD_REQUEST, ""),
    PARSING_INVALID_HEADER(1002, "SOAP Header parsing error", "Error while parsing payload. The SOAP header in payload is invalid: {0}", HttpStatus.BAD_REQUEST, ""),
    PARSING_INVALID_BODY(1003, "SOAP Body parsing error", "Error while parsing payload. The SOAP body in payload is invalid: {0}", HttpStatus.BAD_REQUEST, ""),
    PARSING_INVALID_XML_NODES(1004, "XML parsing error", "Error while parsing payload. The list of nodes extracted from document must be greater than zero, but currently it is zero.", HttpStatus.BAD_REQUEST, ""),
    PARSING_INVALID_ZIPPED_PAYLOAD(1005, "ZIP extraction error", "Error while parsing payload. Cannot unzip payload correctly.", HttpStatus.BAD_REQUEST, ""),
    PARSING_PRIMITIVE_NOT_VALID(1006, "Primitive not valid", "Error while checking primitive. Primitive [{0}] not valid.", HttpStatus.NOT_ACCEPTABLE, ""),
    VALIDATION_INVALID_MULTIBENEFICIARY_CART(1100, "RPTs not valid", "Error while generating payment position for GPD service. The cart is defined as multi-beneficiary but there are a number of RPTs lower than 2.", HttpStatus.BAD_REQUEST, ""),
    VALIDATION_INVALID_IBANS(1101, "IBANs not valid", "Error while generating payment position for GPD service. The IBAN field must be set if digital stamp is not defined for the transfer.", HttpStatus.BAD_REQUEST, ""),
    VALIDATION_INVALID_DEBTOR(1102, "Debtor subject not valid", "Error while generating payment position for GPD service. The debtor subject information is different between the various RPT of the cart.", HttpStatus.BAD_REQUEST, ""),
    VALIDATION_INVALID_CREDITOR_INSTITUTION(1103, "Creditor institution not valid", "Error while generating payment position for GPD service. The creditor institution information is different between the various RPT of the cart.", HttpStatus.BAD_REQUEST, ""),
    VALIDATION_INVALID_RPT(1104, "RPTs not valid", "Error while generating payment position for GPD service. Cannot extract IUV from passed RPT.", HttpStatus.BAD_REQUEST, ""),
    CONFIGURATION_INVALID_CACHE(1200, "Cache not valid", "Error while retrieving data from cache configuration. No valid cached configuration found.", HttpStatus.NOT_FOUND, ""),
    CONFIGURATION_INVALID_STATION(1201, "Station not valid", "Error while retrieving data from cached configuration. No valid station found with code [{0}].", HttpStatus.NOT_FOUND, ""),
    CONFIGURATION_INVALID_CREDITOR_INSTITUTION_STATION(1202, "Creditor institution's station not valid", "Error while retrieving data from cached configuration. No valid station with segregation code [{0}] found in creditor institution with code [{1}].", HttpStatus.NOT_FOUND, ""),
    CONFIGURATION_INVALID_STATION_REDIRECT_URL(1203, "Station not correctly configured", "Error while retrieving data from cached configuration. The station with code [{0}] contains wrong redirect URL and it is not possible to generate valid URI.", HttpStatus.UNPROCESSABLE_ENTITY, ""),
    CONFIGURATION_INVALID_STATION_SERVICE_URL(1204, "Station not correctly configured", "Error while retrieving data from cached configuration. The station with code [{0}] contains wrong on missing service URL configuration and it is not possible to generate valid URI.", HttpStatus.UNPROCESSABLE_ENTITY, ""),
    CONFIGURATION_INVALID_GPD_STATION(1205, "Station not correctly configured for GPD", "Error while retrieving data from cached configuration. The station with code [{0}] is onboarded on GPD but the primitive version is set to 1.", HttpStatus.UNPROCESSABLE_ENTITY, ""),
    PAYMENT_POSITION_NOT_IN_PAYABLE_STATE(1300, "Existing payment position not in payable state", "Error while generating payment position. One or more of the payment position(s) associated to IUV(s) [{0}] exists but are not in a payable state. All the payments are declared invalid.", HttpStatus.CONFLICT, ""),
    PAYMENT_POSITION_IN_INVALID_STATE(1301, "Existing payment position in invalid state", "Error while generating payment position. There is a payment position associated to IUV [{0}] but it is in an invalid state or it corrupted.", HttpStatus.CONFLICT, ""),
    PAYMENT_POSITION_NOT_VALID(1301, "Existing payment position not valid", "Error while generating payment position. The payment position associated to IUV [{0}] is not valid.", HttpStatus.BAD_REQUEST, ""),
    PAYMENT_POSITION_NOT_EXTRACTABLE(1302, "Existing payment position not valid", "Error while extracting payment position. An error occurred while trying to extract payment option.", HttpStatus.BAD_REQUEST, ""),
    PAYMENT_OPTION_NOT_EXTRACTABLE(1303, "Impossible to extract payment option", "Error while extracting payment option. An error occurred while trying to extract payment option.", HttpStatus.BAD_REQUEST, ""),
    RECEIPT_KO_NOT_GENERATED(1400, "KO Receipt not generated", "Error while generating KO receipt. It is not possible to generate the receipt due to an error: [{0}].", HttpStatus.UNPROCESSABLE_ENTITY, ""),
    // --- DB and storage interaction errors ---
    PERSISTENCE_RPT_NOT_FOUND(2000, "RPT not found", "Error while retrieving RPT. RPT with sessionId [{0}] not found.", HttpStatus.NOT_FOUND, ""),
    PERSISTENCE_REQUESTID_CACHING_ERROR(2001, "RequestID caching error", "Error while reading cached RequestID. No valid value found for key [{0}].", HttpStatus.UNPROCESSABLE_ENTITY, ""),
    PERSISTENCE_MAPPING_NAV_TO_IUV_ERROR(2002, "Mapping caching error", "Error while reading cached mapping from NAV to IUV. No valid value found for NAV-based key [{0}].", HttpStatus.UNPROCESSABLE_ENTITY, ""),
    // --- Client errors ---
    CLIENT_APICONFIGCACHE(3000, "APIConfig cache client error", "Error while communicating with APIConfig cache service. {0}", HttpStatus.EXPECTATION_FAILED, ""),
    CLIENT_GPD(3001, "GPD client error", "Error while communicating with GPD service. {0}", HttpStatus.EXPECTATION_FAILED, ""),
    CLIENT_GPD_DEBT_POSITION_NOT_FOUND(404, "Debt position not found", "", HttpStatus.NOT_FOUND, "The debt position was not found"),
    CLIENT_IUVGENERATOR(3002, "IUV Generator client error", "Error while communicating with IUV Generator service. {0}", HttpStatus.EXPECTATION_FAILED, ""),
    CLIENT_DECOUPLER_CACHING(3003, "Decoupler caching client error", "Error while communicating with decoupler caching API. {0}", HttpStatus.EXPECTATION_FAILED, ""),
    CLIENT_CHECKOUT(3004, "Checkout error", "Error while communicating with Checkout service. {0}", HttpStatus.EXPECTATION_FAILED, ""),
    CLIENT_PAAINVIART(3005, "PaaInviaRT error", "Error while communicating with Station for paaInviaRT service. {0}", HttpStatus.EXPECTATION_FAILED, "An error occurred during the communication with the creditor institution's station regarding the sending of a paaInviaRT request."),
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
