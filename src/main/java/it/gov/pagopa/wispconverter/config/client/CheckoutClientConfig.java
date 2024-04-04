package it.gov.pagopa.wispconverter.config.client;

import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.client.checkout.CheckoutClient;
import it.gov.pagopa.wispconverter.util.client.checkout.CheckoutClientLogging;
import it.gov.pagopa.wispconverter.util.client.checkout.CheckoutClientResponseErrorHandler;
import it.gov.pagopa.wispconverter.util.client.gpd.GpdClient;
import it.gov.pagopa.wispconverter.util.client.gpd.GpdClientLogging;
import it.gov.pagopa.wispconverter.util.client.gpd.GpdClientResponseErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CheckoutClientConfig {

    @Value("${client.checkout.read-timeout}")
    private Integer readTimeout;

    @Value("${client.checkout.connect-timeout}")
    private Integer connectTimeout;

    @Value("${client.checkout.base-path}")
    private String basePath;

    @Value("${client.checkout.api-key}")
    private String apiKey;

    @Value("${log.client.checkout.request.include-headers}")
    private boolean clientRequestIncludeHeaders;
    @Value("${log.client.checkout.request.include-payload}")
    private boolean clientRequestIncludePayload;
    @Value("${log.client.checkout.request.max-payload-length}")
    private int clientRequestMaxLength;
    @Value("${log.client.checkout.response.include-headers}")
    private boolean clientResponseIncludeHeaders;
    @Value("${log.client.checkout.response.include-payload}")
    private boolean clientResponseIncludePayload;
    @Value("${log.client.checkout.response.max-payload-length}")
    private int clientResponseMaxLength;

    @Value("${log.client.checkout.mask.header.name}")
    private String maskHeaderName;

    @Value("${log.client.checkout.request.pretty}")
    private boolean clientRequestPretty;

    @Value("${log.client.checkout.response.pretty}")
    private boolean clientResponsePretty;

    @Bean
    public CheckoutClient checkoutClient(ReService reService) {
        CheckoutClientLogging clientLogging = new CheckoutClientLogging();
        clientLogging.setRequestIncludeHeaders(clientRequestIncludeHeaders);
        clientLogging.setRequestIncludePayload(clientRequestIncludePayload);
        clientLogging.setRequestMaxPayloadLength(clientRequestMaxLength);
        clientLogging.setRequestHeaderPredicate(p -> !p.equals(maskHeaderName));
        clientLogging.setRequestPretty(clientRequestPretty);

        clientLogging.setResponseIncludeHeaders(clientResponseIncludeHeaders);
        clientLogging.setResponseIncludePayload(clientResponseIncludePayload);
        clientLogging.setResponseMaxPayloadLength(clientResponseMaxLength);
        clientLogging.setResponsePretty(clientResponsePretty);


        CheckoutClient client = new CheckoutClient(readTimeout, connectTimeout);
        client.addCustomLoggingInterceptor(clientLogging);
        client.addCustomErrorHandler(new CheckoutClientResponseErrorHandler());

        client.setBasePath(basePath);
        client.setBasePath(apiKey);

        return client;
    }
}
