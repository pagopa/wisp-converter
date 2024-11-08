package it.gov.pagopa.wispconverter.service.model.re;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@Builder
@Getter
public class ReResponseContext {
    
    private String payload;
    private HttpHeaders headers;
    private HttpStatus statusCode;
}
