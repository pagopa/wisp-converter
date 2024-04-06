package it.gov.pagopa.wispconverter.config;

import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.client.ServerLoggingProperties;
import it.gov.pagopa.wispconverter.util.filter.AppServerLoggingFilterFilter;
import it.gov.pagopa.wispconverter.util.filter.RequestIdFilter;
import it.gov.pagopa.wispconverter.util.filter.RequestResponseWrapperFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class FilterConfiguration {


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
    @ConfigurationProperties(prefix = "log.server")
    public ServerLoggingProperties serverLoggingProperties() {
        return new ServerLoggingProperties();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE+2)
    public AppServerLoggingFilterFilter appServerLogging() {
        ServerLoggingProperties serverLoggingProperties = serverLoggingProperties();

        AppServerLoggingFilterFilter filter = new AppServerLoggingFilterFilter(serverLoggingProperties);
        filter.setExcludeUrlPatterns(excludeUrlPatterns);
        return filter;
    }

//    @Bean
//    @Order(Ordered.HIGHEST_PRECEDENCE+3)
//    public ReFilter reFilter() {
//        ReFilter filter = new ReFilter(reService);
//        filter.setExcludeUrlPatterns(excludeUrlPatterns);
//        return filter;
//    }


}
