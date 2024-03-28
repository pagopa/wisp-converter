package it.gov.pagopa.wispconverter.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AppErrorCodeMessageEnum {
    UNKNOWN("0000", "UNKNOWN.error", HttpStatus.INTERNAL_SERVER_ERROR),
    ERROR("0500", "system.error", HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_REQUEST("0400", "bad.request", HttpStatus.BAD_REQUEST),

    PARSING_("1000", "", HttpStatus.BAD_REQUEST),
    PARSING_JAXB_EMPTY_NODE_ELEMENT("1001", "jaxb.node-element.empty", HttpStatus.BAD_REQUEST),
    PARSING_JAXB_PARSE_ERROR("1002", "jaxb.parse", HttpStatus.BAD_REQUEST),

    PERSISTENCE_("2000", "", HttpStatus.BAD_REQUEST),

    CLIENT_("3000", "", HttpStatus.BAD_REQUEST),

    ;

    private final String errorCode;
    private final String errorMessageKey;
    private final HttpStatus httpStatus;

    AppErrorCodeMessageEnum(String errorCode, String errorMessageKey, HttpStatus httpStatus) {
        this.errorCode = errorCode;
        this.errorMessageKey = errorMessageKey;
        this.httpStatus = httpStatus;
    }
}
