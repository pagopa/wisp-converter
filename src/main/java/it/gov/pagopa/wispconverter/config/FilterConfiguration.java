package it.gov.pagopa.wispconverter.config;

import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.filter.ReFilter;
import it.gov.pagopa.wispconverter.util.filter.RequestIdFilter;
import it.gov.pagopa.wispconverter.util.filter.RequestResponseWrapperFilter;
import it.gov.pagopa.wispconverter.util.filter.AppServerLoggingFilterFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class FilterConfiguration {

    private final ReService reService;

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

    @Value("${filter.exclude-url-patterns}")
    private List<String> excludeUrlPatterns;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public RequestIdFilter requestIdFilter() {
        RequestIdFilter filter = new RequestIdFilter();
        filter.setExcludeUrlPatterns(excludeUrlPatterns);
        return filter;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE+1)
    public RequestResponseWrapperFilter requestResponseWrapperFilter() {
        RequestResponseWrapperFilter filter = new RequestResponseWrapperFilter();
        filter.setExcludeUrlPatterns(excludeUrlPatterns);
        return filter;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE+2)
    public AppServerLoggingFilterFilter appServerLogging() {
        AppServerLoggingFilterFilter filter = new AppServerLoggingFilterFilter();

        filter.setRequestIncludeHeaders(serverRequestIncludeHeaders);
        filter.setRequestIncludeClientInfo(serverRequestIncludeClientInfo);
        filter.setRequestIncludePayload(serverRequestIncludePayload);
        filter.setRequestMaxPayloadLength(serverRequestMaxLength);
        filter.setRequestPretty(serverRequestPretty);

        filter.setResponseIncludeHeaders(serverResponseIncludeHeaders);
        filter.setResponseIncludePayload(serverResponseIncludePayload);
        filter.setResponseMaxPayloadLength(serverResponseMaxLength);
        filter.setResponsePretty(serverResponsePretty);

        filter.setExcludeUrlPatterns(excludeUrlPatterns);
        return filter;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE+3)
    public ReFilter reFilter() {
        ReFilter filter = new ReFilter(reService);
        filter.setExcludeUrlPatterns(excludeUrlPatterns);
        return filter;
    }


}
