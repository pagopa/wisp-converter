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
        log.info("Reconciliation CRON JOB: recoverReceiptKOCronJob running at {}", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));

        ZonedDateTime dateFrom = ZonedDateTime.now(ZoneOffset.UTC).minusHours(FROM_N_HOURS_AGO);
        ZonedDateTime dateTo = ZonedDateTime.now(ZoneOffset.UTC).minusHours(UNTIL_N_HOURS_AGO);
        int receiptSize = recoveryService.recoverReceiptKOAll(dateFrom, dateTo);

        log.info("Reconciliation CRON JOB: recoverReceiptKOCronJob {} receipt-ko sent", receiptSize);
        this.threadOfExecution = Thread.currentThread();
    }
}
