package it.gov.pagopa.wispconverter.scheduler;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import it.gov.pagopa.wispconverter.service.RecoveryService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.ZonedDateTime;

@SpringBootTest(classes = RecoveryScheduler.class)
class RecoverySchedulerTest {
    @MockBean
    @Qualifier("recoveryService")
    private RecoveryService recoveryService;

    @Autowired
    @InjectMocks
    RecoveryScheduler recoveryScheduler;

    @Test
    void testRecoverReceiptKOCronJob() {
        // Arrange
        when(recoveryService.recoverReceiptKOAll(any(), any())).thenReturn(5);
        when(recoveryService.recoverMissingRedirect(any(), any())).thenReturn(3);

        // Act
        recoveryScheduler.recoverReceiptKOCronJob();

        // Assert
        verify(recoveryService, times(1)).recoverReceiptKOAll(any(ZonedDateTime.class), any(ZonedDateTime.class));
        verify(recoveryService, times(1)).recoverMissingRedirect(any(ZonedDateTime.class), any(ZonedDateTime.class));

        assertNotNull(recoveryScheduler.getThreadOfExecution());
    }
}
