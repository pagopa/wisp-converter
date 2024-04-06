package it.gov.pagopa.wispconverter.util.client;

import lombok.Data;

@Data
public class ClientLoggingProperties {


    private Request request = new Request();
    private Response response = new Response();

    @Data
    public static class Request {
        private String maskHeaderName;
        private boolean includeHeaders;
        private boolean includePayload;
        private Integer maxPayloadLength;
        private boolean pretty;
    }

    @Data
    public static class Response {
        private boolean includeHeaders;
        private boolean includePayload;
        private Integer maxPayloadLength;
        private boolean pretty;
    }


}
