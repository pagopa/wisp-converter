package it.gov.pagopa.wispconverter.config;

import it.gov.pagopa.wispconverter.service.ConfigCacheService;
import it.gov.pagopa.wispconverter.service.ConfigStandInService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.ZonedDateTime;

@Configuration
@Slf4j
@EnableScheduling
public class ScheduledJobsConfig {

    private final ConfigStandInService configStandInService;
    private final ConfigCacheService configCacheService;

    public ScheduledJobsConfig(ConfigStandInService configStandInService, ConfigCacheService configCacheService) {
        this.configStandInService = configStandInService;
        this.configCacheService = configCacheService;
    }

    @Scheduled(cron = "${wisp-converter.refresh.standin.cron:-}")
    @EventListener(ApplicationReadyEvent.class)
    public void refreshStandIn() {
        log.info("[Scheduled] Starting configuration standin refresh {}", ZonedDateTime.now());
        configStandInService.getCache();
    }

    @Scheduled(cron = "${wisp-converter.refresh.cache.cron:-}")
    @EventListener(ApplicationReadyEvent.class)
    public void refreshCache() {
        log.info("[Scheduled] Starting configuration cache refresh {}", ZonedDateTime.now());
        configCacheService.getCache();
    }

}
