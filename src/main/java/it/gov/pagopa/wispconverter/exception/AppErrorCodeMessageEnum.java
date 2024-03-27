package it.gov.pagopa.wispconverter.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AppErrorCodeMessageEnum {
  ERROR("0500", "system.error", HttpStatus.INTERNAL_SERVER_ERROR),
  BAD_REQUEST("0400", "bad.request", HttpStatus.BAD_REQUEST);

  private final String errorCode;
  private final String errorMessageKey;
  private final HttpStatus httpStatus;

  AppErrorCodeMessageEnum(String errorCode, String errorMessageKey, HttpStatus httpStatus) {
    this.errorCode = errorCode;
    this.errorMessageKey = errorMessageKey;
    this.httpStatus = httpStatus;
  }
}
