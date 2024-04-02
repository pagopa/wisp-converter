package it.gov.pagopa.wispconverter.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum AppErrorCodeMessageEnum {
    ERROR(500, "System error", "{0}", HttpStatus.INTERNAL_SERVER_ERROR),
    PARSING_GENERIC_ERROR(1000, "Parse error", "Error while parsing payload. Generic error: {0}", HttpStatus.BAD_REQUEST),
    PARSING_INVALID_HEADER(1001, "Parse error", "Error while parsing payload. Generic error: {0}", HttpStatus.BAD_REQUEST),
    PARSING_INVALID_BODY(1002, "Parse error", "Error while parsing payload. Generic error: {0}", HttpStatus.BAD_REQUEST),


    PARSING_INVALID_ZIPPED_PAYLOAD(1001, "Parse error", "Error while parsing payload. Cannot unzip payload correctly.", HttpStatus.BAD_REQUEST),
    PARSING_PRIMITIVE_NOT_VALID(1002, "Primitive not valid", "Primitive [{0}] not valid", HttpStatus.NOT_ACCEPTABLE),
    PERSISTENCE_RPT_NOT_FOUND(2000, "RPT not found", "RPT with sessionId [{0}] not found", HttpStatus.NOT_FOUND),
    CLIENT_IUV_GENERATOR(3003, "IUVGeneratorClient error", "IUVGeneratorClient status [{0}] - {1}", HttpStatus.EXPECTATION_FAILED),
    CLIENT_GPD(3004, "GPDClient error", "GPDClient status [{0}] - {1}", HttpStatus.EXPECTATION_FAILED),
    CLIENT_DECOUPLER_CACHING(3005, "DecouplerCachingClient error", "DecouplerCachingClient status [{0}] - {1}", HttpStatus.EXPECTATION_FAILED),
    CLIENT_CHECKOUT(3006, "Checkout error", "CheckoutClient status [{0}] - {1}", HttpStatus.EXPECTATION_FAILED),
    CLIENT_(3000, "", "", HttpStatus.BAD_REQUEST),
    LOGIC_(4000, "", "", HttpStatus.BAD_REQUEST),
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
