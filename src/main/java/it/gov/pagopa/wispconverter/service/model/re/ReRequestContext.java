package it.gov.pagopa.wispconverter.service.model.re;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

@Builder
@Getter
public class ReRequestContext {

    private HttpMethod method;
    private String uri;
    private HttpHeaders headers;
    private String payload;
}
