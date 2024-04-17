package it.gov.pagopa.wispconverter.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum AppErrorCodeMessageEnum {
    ERROR(500, "System error", "{0}", HttpStatus.INTERNAL_SERVER_ERROR),
    // --- Internal logic errors ---
    GENERIC_ERROR(1000, "Generic flow error", "Error while executing conversion flow. {0}", HttpStatus.UNPROCESSABLE_ENTITY),
    PARSING_GENERIC_ERROR(1001, "Generic parsing error", "Error while parsing payload. {0}", HttpStatus.BAD_REQUEST),
    PARSING_INVALID_HEADER(1002, "SOAP Header parsing error", "Error while parsing payload. The SOAP header in payload is invalid: {0}", HttpStatus.BAD_REQUEST),
    PARSING_INVALID_BODY(1003, "SOAP Body parsing error", "Error while parsing payload. The SOAP body in payload is invalid: {0}", HttpStatus.BAD_REQUEST),
    PARSING_INVALID_XML_NODES(1004, "XML parsing error", "Error while parsing payload. The list of nodes extracted from document must be greater than zero, but currently it is zero.", HttpStatus.BAD_REQUEST),
    PARSING_INVALID_ZIPPED_PAYLOAD(1005, "ZIP extraction error", "Error while parsing payload. Cannot unzip payload correctly.", HttpStatus.BAD_REQUEST),
    PARSING_PRIMITIVE_NOT_VALID(1006, "Primitive not valid", "Error while checking primitive. Primitive [{0}] not valid.", HttpStatus.NOT_ACCEPTABLE),
    VALIDATION_INVALID_MULTIBENEFICIARY_CART(1100, "RPTs not valid", "Error while generating debt position for GPD service. The cart is defined as multi-beneficiary but there are a number of RPTs lower than 2.", HttpStatus.BAD_REQUEST),
    VALIDATION_INVALID_IBANS(1101, "IBANs not valid", "Error while generating debt position for GPD service. The IBAN field must be set if digital stamp is not defined for the transfer.", HttpStatus.BAD_REQUEST),
    VALIDATION_INVALID_DEBTOR(1102, "Debtor subject not valid", "Error while generating debt position for GPD service. The debtor subject information is different between the various RPT of the cart.", HttpStatus.BAD_REQUEST),
    VALIDATION_INVALID_CREDITOR_INSTITUTION(1103, "Creditor institution not valid", "Error while generating debt position for GPD service. The creditor institution information is different between the various RPT of the cart.", HttpStatus.BAD_REQUEST),
    CONFIGURATION_INVALID_STATION(1200, "Station not valid", "Error while generating cart for Checkout service. No valid station found with code [{0}].", HttpStatus.NOT_FOUND),
    CONFIGURATION_INVALID_STATION_REDIRECT_URL(1201, "Station not valid", "Error while generating cart for Checkout service. The station with code [{0}] contains wrong redirect URL and it is not possible to generate valid URI.", HttpStatus.NOT_FOUND),
    CONFIGURATION_INVALID_CACHE(1202, "Cache not valid", "Error while reading configuration cache. No valid cached configuration found.", HttpStatus.NOT_FOUND),
    // --- DB and storage interaction errors ---
    PERSISTENCE_RPT_NOT_FOUND(2000, "RPT not found", "Error while retrieving RPT. RPT with sessionId [{0}] not found.", HttpStatus.NOT_FOUND),
    PERSISTENCE_REQUESTID_CACHING_ERROR(2001, "RequestID caching error", "Error while reading cached RequestID. No valid value found for key [{0}].", HttpStatus.UNPROCESSABLE_ENTITY),
    PERSISTENCE_MAPPING_NAV_TO_IUV_ERROR(2002, "Mapping caching error", "Error while reading cached mapping from NAV to IUV. No valid value found for NAV-based key [{0}].", HttpStatus.UNPROCESSABLE_ENTITY),
    // --- Client errors ---
    CLIENT_APICONFIGCACHE(3000, "APIConfig cache client error", "Error while communicating with APIConfig cache service. {0}", HttpStatus.EXPECTATION_FAILED),
    CLIENT_GPD(3001, "GPD client error", "Error while communicating with GPD service. {0}", HttpStatus.EXPECTATION_FAILED),
    CLIENT_IUVGENERATOR(3002, "IUV Generator client error", "Error while communicating with IUV Generator service. {0}", HttpStatus.EXPECTATION_FAILED),
    CLIENT_DECOUPLER_CACHING(3003, "Decoupler caching client error", "Error while communicating with decoupler caching API. {0}", HttpStatus.EXPECTATION_FAILED),
    CLIENT_CHECKOUT(3004, "Checkout error", "Error while communicating with Checkout service. {0}", HttpStatus.EXPECTATION_FAILED),
    CLIENT_CHECKOUT_NO_REDIRECT_LOCATION(3005, "Checkout redirect error", "Error while communicating with Checkout service. No valid 'Location' header was found,", HttpStatus.EXPECTATION_FAILED),
    CLIENT_CHECKOUT_INVALID_REDIRECT_LOCATION(3006, "Checkout redirect error", "Error while communicating with Checkout service. An empty 'Location' header was found.", HttpStatus.EXPECTATION_FAILED),

    ;

    private final Integer code;
    private final String title;
    private final String detail;
    private final HttpStatusCode status;

    AppErrorCodeMessageEnum(Integer code, String title, String detail, HttpStatus status) {
        this.code = code;
        this.title = title;
        this.detail = detail;
        this.status = status;
    }
}
