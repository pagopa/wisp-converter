package it.gov.pagopa.wispconverter.scheduler;

import it.gov.pagopa.wispconverter.service.RecoveryService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
@ConditionalOnProperty(name = "cron.job.schedule.recovery.enabled", matchIfMissing = false)
public class RecoveryScheduler {

    private final RecoveryService recoveryService;
    @Value("${cron.job.schedule.recovery.hours.ago.from}")
    private int fromHoursAgo;
    @Value("${cron.job.schedule.recovery.hours.ago.until}") // untilHoursAgo: upperbound for a payment session
    private int untilHoursAgo;
    @Getter
    private Thread threadOfExecution;

    @Autowired
    public RecoveryScheduler(RecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    // https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/support/CronExpression.html
    @Scheduled(cron = "${cron.job.schedule.recovery.receipt-ko.trigger}")
    @Async
    public void recoverReceiptKOCronJob() {
        ZonedDateTime dateFrom = ZonedDateTime.now(ZoneOffset.UTC).minusHours(fromHoursAgo).truncatedTo(ChronoUnit.HOURS);
        ZonedDateTime dateTo = ZonedDateTime.now(ZoneOffset.UTC).minusHours(untilHoursAgo).truncatedTo(ChronoUnit.HOURS);
        log.info("[WISP-Recovery][Scheduled][Start] Reconciliation Cron: recoverReceiptKOCronJob running at {}, for recover stale RPT from {} to {}",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()), dateFrom, dateTo);

        // recover RPT without redirect
        int missingRedirectRecovered = this.recoveryService.recoverMissingRedirect(dateFrom, dateTo);

        // recover receipt-rt in state redirect or sending with rt equals to null
        int missingRTRecovered = this.recoveryService.recoverReceiptKOByDate(dateFrom, dateTo).getPayments().size();

        log.info("[WISP-Recovery][Scheduled][Stop] Reconciliation Cron: recoverReceiptKOCronJob {} receipt-ko sent," +
                " missingRedirect: {}, missingRTRecovered: {}", missingRedirectRecovered + missingRTRecovered, missingRedirectRecovered, missingRTRecovered);
        this.threadOfExecution = Thread.currentThread();
    }
}
