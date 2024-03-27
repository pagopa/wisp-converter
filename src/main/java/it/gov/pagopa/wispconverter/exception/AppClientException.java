package it.gov.pagopa.wispconverter.exception;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class AppClientException extends AppException {

  private final int httpStatusCode;

  public AppClientException(
      int httpStatusCode, AppErrorCodeMessageEnum codeMessage, Serializable... args) {
    super(codeMessage, args);
    this.httpStatusCode = httpStatusCode;
  }
}
