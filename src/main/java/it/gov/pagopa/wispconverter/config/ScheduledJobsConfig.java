package it.gov.pagopa.wispconverter.config;

import it.gov.pagopa.wispconverter.service.ConfigCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.ZonedDateTime;

@Configuration
@Slf4j
@EnableScheduling
public class ScheduledJobsConfig {

    private final ConfigCacheService configCacheService;
    public ScheduledJobsConfig(ConfigCacheService configCacheService){
        this.configCacheService = configCacheService;
    }

    @Scheduled(cron = "${wisp-converter-cache.refresh.cron:-}")
    public void refreshCache() {
        log.info("[Scheduled] Starting configuration refresh {}", ZonedDateTime.now());
        configCacheService.loadCache();
    }

}
