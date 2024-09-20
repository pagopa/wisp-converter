package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.controller.model.RecoveryReceiptResponse;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.RTRepository;
import it.gov.pagopa.wispconverter.repository.ReEventRepository;
import it.gov.pagopa.wispconverter.repository.model.RTEntity;
import it.gov.pagopa.wispconverter.repository.model.ReEventEntity;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class RecoveryServiceTest {

    @Mock
    private RTRepository rtRepository;

    @Mock
    private ReEventRepository reRepository;

    @Mock
    private ReceiptService receiptService;

    @Mock
    private ReService reService;

    @InjectMocks
    private RecoveryService recoveryService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        recoveryService.receiptGenerationWaitTime = 60L;
        recoveryService.requestIDMappingTTL = 1440L;
    }

    @Test
    void testRecoverReceiptKO() {
        // Arrange
        String dateFrom = "2024-09-05";
        String dateTo = "2024-09-09";
        String ci = "ci";
        String iuv = "iuv";
        String ccp = "cpp";
        String session = "sessionId";
        List<ReEventEntity> rtSuccessReEventEntity = List.of();

        when(reRepository.findBySessionIdAndStatus(anyString(), anyString(), anyString(), anyString())).thenReturn(rtSuccessReEventEntity);
        doNothing().when(receiptService)
                .sendRTKoFromSessionId(anyString(), any());
        doNothing().when(reService).addRe(any(ReEventDto.class));

        // Act
        recoveryService.recoverReceiptKO(ci, iuv, ccp, session, dateFrom, dateTo);

        // Assert
        verify(reRepository, times(1)).findBySessionIdAndStatus(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testRecoverReceiptKOForCreditorInstitution_Success() {
        // Arrange
        String creditorInstitution = "77777777777";
        String dateFrom = "2024-09-05";
        String dateTo = "2024-09-09";
        List<RTEntity> mockRTEntities = List.of();

        when(rtRepository.findByOrganizationId(anyString(), anyString(), anyString())).thenReturn(mockRTEntities);

        // Act
        RecoveryReceiptResponse response = recoveryService.recoverReceiptKOForCreditorInstitution(creditorInstitution, dateFrom, dateTo);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getPayments().size());
    }

    @Test
    void testRecoverReceiptKOForCreditorInstitution_LowerBoundFailure() {
        // Arrange
        String creditorInstitution = "77777777777";
        String dateFrom = "2024-09-01";  // Date earlier than valid start date
        String dateTo = "2024-09-09";

        // Act and Assert
        AppException exception = assertThrows(
                AppException.class, () -> recoveryService.recoverReceiptKOForCreditorInstitution(creditorInstitution, dateFrom, dateTo)
        );
        assertEquals("The lower bound cannot be lower than [2024-09-03]", exception.getMessage());
    }

    @Test
    void testRecoverReceiptKOForCreditorInstitution_UpperBoundFailure() {
        // Arrange
        String creditorInstitution = "77777777777";
        String dateFrom = "2024-09-05";
        String dateTo = LocalDate.now().plusDays(1).toString();  // Future date, should fail

        // Act and Assert
        AppException exception = assertThrows(
                AppException.class, () -> recoveryService.recoverReceiptKOForCreditorInstitution(creditorInstitution, dateFrom, dateTo)
        );
        String expectedMessage = String.format("The upper bound cannot be higher than [%s]", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void testRecoverReceiptKOByIUV() {
        // Arrange
        String dateFrom = "2024-09-05";
        String dateTo = "2024-09-09";
        String ci = "ci";
        String iuv = "iuv";
        List<ReEventEntity> rtSuccessReEventEntity = List.of();

        when(reRepository.findByIuvAndOrganizationId(anyString(), anyString(), anyString(), anyString())).thenReturn(rtSuccessReEventEntity);
        doNothing().when(receiptService).sendRTKoFromSessionId(anyString(), any());
        doNothing().when(reService).addRe(any(ReEventDto.class));

        // Act
        recoveryService.recoverReceiptKO(ci, iuv, dateFrom, dateTo);

        // Assert
        verify(reRepository, times(1)).findByIuvAndOrganizationId(anyString(), anyString(), anyString(), anyString());
    }
}