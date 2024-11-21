package it.gov.pagopa.wispconverter.config;


import it.gov.pagopa.wispconverter.util.filter.RequestIdFilter;
import it.gov.pagopa.wispconverter.util.filter.RequestResponseWrapperFilter;
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
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public RequestResponseWrapperFilter requestResponseWrapperFilter() {
        RequestResponseWrapperFilter filter = new RequestResponseWrapperFilter();
        filter.setExcludeUrlPatterns(excludeUrlPatterns);
        return filter;
    }

}
