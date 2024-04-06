package it.gov.pagopa.wispconverter.config.client;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

@Data
public class ClientLoggingProperties {


    private Request request = new Request();
    private Response response = new Response();

    @Data
    public static class Request {
        private String maskHeaderName;
        private boolean includeHeaders;
        private boolean includePayload;
        private int maxPayloadLength;
        private boolean pretty;
    }

    @Data
    public static class Response {
        private boolean includeHeaders;
        private boolean includePayload;
        private int maxPayloadLength;
        private boolean pretty;
    }


}
