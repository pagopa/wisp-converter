
package it.gov.pagopa.wispconverter.util.client;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RequestResponseLoggingProperties {


    private Request request = new Request();
    private Response response = new Response();

    @Setter
    @Getter
    public static class Request {
        private String maskHeaderName;
        private boolean includeHeaders;
        private boolean includePayload;
        private Integer maxPayloadLength;
        private boolean pretty;
    }

    @Setter
    @Getter
    public static class Response {
        private boolean includeHeaders;
        private boolean includePayload;
        private Integer maxPayloadLength;
        private boolean pretty;
    }


}
