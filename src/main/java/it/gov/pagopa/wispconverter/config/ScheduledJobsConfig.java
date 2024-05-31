package it.gov.pagopa.wispconverter.config;

import it.gov.pagopa.wispconverter.service.ConfigCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.ZonedDateTime;

@Configuration
@Slf4j
@EnableScheduling
@ConditionalOnExpression("'${info.properties.environment}'!='test'")
public class ScheduledJobsConfig {

    private final ConfigCacheService configCacheService;

    public ScheduledJobsConfig(ConfigCacheService configCacheService) {
        this.configCacheService = configCacheService;
    }

    @Scheduled(cron = "${wisp-converter.refresh.cache.cron:-}")
    @EventListener(ApplicationReadyEvent.class)
    public void refreshCache() {
        log.info("[Scheduled] Starting configuration cache refresh {}", ZonedDateTime.now());
        configCacheService.refreshCache();
    }

}
