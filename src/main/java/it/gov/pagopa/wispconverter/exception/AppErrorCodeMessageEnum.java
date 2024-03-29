package it.gov.pagopa.wispconverter.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum AppErrorCodeMessageEnum {
    UNKNOWN("0000", "Unknown error", HttpStatus.INTERNAL_SERVER_ERROR),
    ERROR("0500", "System error",  HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_REQUEST("0400", "Bad Request",  HttpStatus.BAD_REQUEST),
    PARSING_("1000", "a", HttpStatus.BAD_REQUEST),
    PARSING_JAXB_EMPTY_NODE_ELEMENT("1001", "JAXB node element is empty", HttpStatus.BAD_REQUEST),
    PARSING_JAXB_PARSE_ERROR("1002", "JAXB error during parse", HttpStatus.BAD_REQUEST),
    PERSISTENCE_("2000", "Persistence error", HttpStatus.BAD_REQUEST),
    CLIENT_("3000", "cvv", HttpStatus.BAD_REQUEST),
    ;

    private final String code;
    private final String title;
    private final HttpStatusCode status;

    AppErrorCodeMessageEnum(String code, String title, HttpStatus status) {
        this.code = code;
        this.title = title;
        this.status = status;
    }
}
