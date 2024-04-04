package it.gov.pagopa.wispconverter.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum AppErrorCodeMessageEnum {
    ERROR                   ( 500, "System error",                  "{0}",                                      HttpStatus.INTERNAL_SERVER_ERROR),
    PARSE_ERROR             (1000, "Parse error",                   "Error while parsing: {0}",                 HttpStatus.BAD_REQUEST),
    PRIMITIVE_NOT_VALID     (1001, "Primitive not valid",           "Primitive [{0}] not valid",                HttpStatus.NOT_ACCEPTABLE),
    RPT_NOT_FOUND           (1002, "RPT not found",                 "RPT with sessionId [{0}] not found",       HttpStatus.NOT_FOUND),
    CLIENT_IUV_GENERATOR    (1003, "IUVGeneratorClient error",      "IUVGeneratorClient status [{0}] - {1}",    HttpStatus.EXPECTATION_FAILED),
    CLIENT_GPD              (1004, "GPDClient error",               "GPDClient status [{0}] - {1}",             HttpStatus.EXPECTATION_FAILED),
    CLIENT_DECOUPLER_CACHING(1005, "DecouplerCachingClient error",  "DecouplerCachingClient status [{0}] - {1}",HttpStatus.EXPECTATION_FAILED),
    CLIENT_CHECKOUT         (1006, "CheckoutClient error",          "CheckoutClient status [{0}] - {1}",        HttpStatus.EXPECTATION_FAILED),
    UNZIP                   (1007, "Unzip error",                   "{0}",                                      HttpStatus.INTERNAL_SERVER_ERROR),
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
