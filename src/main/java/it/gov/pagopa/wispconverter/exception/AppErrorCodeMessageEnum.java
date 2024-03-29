package it.gov.pagopa.wispconverter.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum AppErrorCodeMessageEnum {
    UNKNOWN("0000", "Unknown error","UNKNOWN.error", HttpStatus.INTERNAL_SERVER_ERROR),
    ERROR("0500", "System error", "system.error", HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_REQUEST("0400", "Bad Request", "bad.request", HttpStatus.BAD_REQUEST),
    PARSING_("1000", "a","a", HttpStatus.BAD_REQUEST),
    PARSING_JAXB_EMPTY_NODE_ELEMENT("1001", "JAXB node element is empty","jaxb.node-element.empty", HttpStatus.BAD_REQUEST),
    PARSING_JAXB_PARSE_ERROR("1002", "JAXB error during parse","jaxb.parse", HttpStatus.BAD_REQUEST),
    PERSISTENCE_("2000", "b","b", HttpStatus.BAD_REQUEST),
    CLIENT_("3000", "cvv","c.c", HttpStatus.BAD_REQUEST),
    ;

    private final String code;
    private final String reason;
    private final String messageDetailCode;
    private final HttpStatusCode status;

    AppErrorCodeMessageEnum(String code, String reason, String messageDetailCode, HttpStatus status) {
        this.code = code;
        this.reason = reason;
        this.messageDetailCode = messageDetailCode;
        this.status = status;
    }
}
