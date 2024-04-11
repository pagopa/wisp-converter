package it.gov.pagopa.wispconverter.util.client;

import lombok.*;

@Setter
@Getter
public class RequestResponseLoggingProperties {


    private Request request = new Request();
    private Response response = new Response();

    @Setter
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String maskHeaderName;
        private boolean includeHeaders;
        private boolean includePayload;
        private Integer maxPayloadLength;
        private boolean pretty;
        private boolean includeClientInfo;
    }

    @Setter
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private boolean includeHeaders;
        private boolean includePayload;
        private Integer maxPayloadLength;
        private boolean pretty;
    }


}
