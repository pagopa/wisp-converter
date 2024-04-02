package it.gov.pagopa.wispconverter.config;

import it.gov.pagopa.wispconverter.util.filter.ReFilter;
import it.gov.pagopa.wispconverter.util.filter.RequestIdFilter;
import it.gov.pagopa.wispconverter.util.filter.RequestResponseWrapperFilter;
import it.gov.pagopa.wispconverter.util.filter.AppServerLoggingFilterFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
public class FilterConfiguration {

    @Value("${log.server.request.include-headers}")
    private boolean serverRequestIncludeHeaders;
    @Value("${log.server.request.include-client-info}")
    private boolean serverRequestIncludeClientInfo;
    @Value("${log.server.request.include-payload}")
    private boolean serverRequestIncludePayload;
    @Value("${log.server.request.max-payload-length}")
    private int serverRequestMaxLength;
    @Value("${log.server.response.include-headers}")
    private boolean serverResponseIncludeHeaders;
    @Value("${log.server.response.include-payload}")
    private boolean serverResponseIncludePayload;
    @Value("${log.server.response.max-payload-length}")
    private int serverResponseMaxLength;

    @Value("${log.server.request.pretty}")
    private boolean serverRequestPretty;

    @Value("${log.server.response.pretty}")
    private boolean serverResponsePretty;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE+1)
    public RequestResponseWrapperFilter requestResponseWrapperFilter() {
        return new RequestResponseWrapperFilter();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE+2)
    public AppServerLoggingFilterFilter appServerLogging() {
        AppServerLoggingFilterFilter appServerLoggingFilterFilter = new AppServerLoggingFilterFilter();

        appServerLoggingFilterFilter.setRequestIncludeHeaders(serverRequestIncludeHeaders);
        appServerLoggingFilterFilter.setRequestIncludeClientInfo(serverRequestIncludeClientInfo);
        appServerLoggingFilterFilter.setRequestIncludePayload(serverRequestIncludePayload);
        appServerLoggingFilterFilter.setRequestMaxPayloadLength(serverRequestMaxLength);
        appServerLoggingFilterFilter.setRequestPretty(serverRequestPretty);

        appServerLoggingFilterFilter.setResponseIncludeHeaders(serverResponseIncludeHeaders);
        appServerLoggingFilterFilter.setResponseIncludePayload(serverResponseIncludePayload);
        appServerLoggingFilterFilter.setResponseMaxPayloadLength(serverResponseMaxLength);
        appServerLoggingFilterFilter.setResponsePretty(serverResponsePretty);
        return appServerLoggingFilterFilter;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE+3)
    public ReFilter reFilter() {
        return new ReFilter();
    }


}
