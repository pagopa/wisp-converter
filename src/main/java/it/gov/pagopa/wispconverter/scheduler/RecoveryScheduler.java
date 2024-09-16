package it.gov.pagopa.wispconverter.scheduler;

import it.gov.pagopa.wispconverter.service.RecoveryService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
@ConditionalOnProperty(name = "cron.job.schedule.recovery.enabled", matchIfMissing = true)
public class RecoveryScheduler {
    private static final int UNTIL_N_HOURS_AGO = 2;
    private static final int FROM_N_HOURS_AGO = 4;

    @Autowired
    RecoveryService recoveryService;

    @Getter
    private Thread threadOfExecution;

    @Scheduled(cron = "${cron.job.schedule.recovery.receipt-ko.trigger}")
    @Async
    public void recoverReceiptKOCronJob() {
        ZonedDateTime dateFrom = ZonedDateTime.now(ZoneOffset.UTC).minusHours(FROM_N_HOURS_AGO);
        ZonedDateTime dateTo = ZonedDateTime.now(ZoneOffset.UTC).minusHours(UNTIL_N_HOURS_AGO);
        log.info("[Scheduled] Reconciliation Cron: recoverReceiptKOCronJob running at {}, for recover stale RPT from {} to {}",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()), dateFrom, dateTo);

        int receiptSize = recoveryService.recoverReceiptKOAll(dateFrom, dateTo);
        int missingRedirectRecovery = recoveryService.recoverMissingRedirect(dateFrom, dateTo);

        log.info("[Scheduled] Reconciliation Cron: recoverReceiptKOCronJob {} receipt-ko sent", receiptSize + missingRedirectRecovery);
        this.threadOfExecution = Thread.currentThread();
    }
}
