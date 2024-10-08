package it.gov.pagopa.wispconverter.scheduler;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import it.gov.pagopa.wispconverter.controller.model.RecoveryReceiptResponse;
import it.gov.pagopa.wispconverter.service.RecoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;
import java.util.List;

@ActiveProfiles(profiles = "test")
@SpringBootTest(classes = RecoveryScheduler.class)
class RecoverySchedulerTest {
    @MockBean
    private RecoveryService recoveryService;

    RecoveryScheduler recoveryScheduler;

    @BeforeEach
    public void setup() {
        recoveryScheduler = new RecoveryScheduler(recoveryService);
    }

    @Test
    void testRecoverReceiptKOCronJob() {
        // Arrange
        when(recoveryService.recoverReceiptKOByDate(any(), any())).thenReturn(RecoveryReceiptResponse.builder().payments(List.of()).build());
        when(recoveryService.recoverMissingRedirect(any(), any())).thenReturn(3);

        // Act
        recoveryScheduler.recoverReceiptKOCronJob();

        // Assert
        verify(recoveryService, times(1)).recoverReceiptKOByDate(any(ZonedDateTime.class), any(ZonedDateTime.class));
        verify(recoveryService, times(1)).recoverMissingRedirect(any(ZonedDateTime.class), any(ZonedDateTime.class));

        assertNotNull(recoveryScheduler.getThreadOfExecution());
    }
}
