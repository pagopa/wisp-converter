package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.controller.model.RecoveryReceiptResponse;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.model.RTEntity;
import it.gov.pagopa.wispconverter.repository.model.ReEventEntity;
import it.gov.pagopa.wispconverter.repository.model.SessionIdEntity;
import it.gov.pagopa.wispconverter.repository.model.enumz.InternalStepStatus;
import it.gov.pagopa.wispconverter.secondary.RTRepositorySecondary;
import it.gov.pagopa.wispconverter.secondary.ReEventRepositorySecondary;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class RecoveryServiceTest {

    @Mock
    private RTRepositorySecondary rtRepository;

    @Mock
    private ReEventRepositorySecondary reRepository;

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
        String ci = "ci";
        String iuv = "iuv";
        String ccp = "cpp";
        String session = "sessionId";

        doNothing().when(receiptService)
                .sendRTKoFromSessionId(anyString(), any());
        doNothing().when(reService).addRe(any(ReEventDto.class));

        // Act
        recoveryService.callSendReceiptKO(ci, iuv, ccp, session);

        // Assert
        verify(receiptService, times(1)).sendRTKoFromSessionId(anyString(), any(InternalStepStatus.class));
    }

    @Test
    void testRecoverReceiptKOAll() {
        // Arrange
        ZonedDateTime dateFrom = ZonedDateTime.now(ZoneOffset.UTC).minusHours(5);
        ZonedDateTime dateTo = ZonedDateTime.now(ZoneOffset.UTC).minusHours(4);
        List<RTEntity> mockRTEntities = List.of();

        when(rtRepository.findByMidReceiptStatusInAndTimestampBetween(anyString(), anyString())).thenReturn(mockRTEntities);

        // Act
        RecoveryReceiptResponse recoveredReceipt = recoveryService.recoverReceiptKOByDate(dateFrom, dateTo);

        // Assert
        assertEquals(0, recoveredReceipt.getPayments().size());
    }

    @Test
    void testRecoverReceiptKOAll_notEmpty() {
        // Arrange
        ZonedDateTime dateFrom = ZonedDateTime.now(ZoneOffset.UTC).minusHours(5);
        ZonedDateTime dateTo = ZonedDateTime.now(ZoneOffset.UTC).minusHours(4);
        List<RTEntity> mockRTEntities = List.of(RTEntity.builder()
                                                        .iuv("iuv")
                                                        .ccp("ccp")
                                                        .domainId("idDominio")
                                                        .build());
        List<ReEventEntity> mockReEventEntities = List.of(ReEventEntity.builder()
                                                                  .ccp("ccp2")
                                                                  .insertedTimestamp(Instant.now())
                                                                  .build());

        when(rtRepository.findByMidReceiptStatusInAndTimestampBetween(anyString(), anyString())).thenReturn(mockRTEntities);
        when(reRepository.findByIuvAndOrganizationId(anyString(), anyString(), anyString(), anyString())).thenReturn(mockReEventEntities);

        // Act
        RecoveryReceiptResponse recoveredReceipt = recoveryService.recoverReceiptKOByDate(dateFrom, dateTo);

        // Assert
        assertEquals(1, recoveredReceipt.getPayments().size());
    }

    @Test
    void testRecoverMissingRedirect() {
        // Arrange
        ZonedDateTime dateFrom = ZonedDateTime.now(ZoneOffset.UTC).minusHours(5);
        ZonedDateTime dateTo = ZonedDateTime.now(ZoneOffset.UTC).minusHours(4);
        List<SessionIdEntity> mockSessionEntities = List.of();
        List<ReEventEntity> mockReEventEntities = List.of();

        when(reRepository.findSessionWithoutRedirect(anyString(), anyString())).thenReturn(mockSessionEntities);
        when(reRepository.findBySessionIdAndStatus(anyString(), anyString(), anyString(), anyString(), anyInt())).thenReturn(mockReEventEntities);

        // Act
        int recoveredReceipt = recoveryService.recoverMissingRedirect(dateFrom, dateTo);

        // Assert
        assertEquals(0, recoveredReceipt);
    }

    @Test
    void testRecoverMissingRedirect_notEmpty() {
        // Arrange
        ZonedDateTime dateFrom = ZonedDateTime.now(ZoneOffset.UTC).minusHours(5);
        ZonedDateTime dateTo = ZonedDateTime.now(ZoneOffset.UTC).minusHours(4);
        List<SessionIdEntity> mockSessionEntities = List.of(SessionIdEntity.builder().sessionId("mockSessionId").build());
        List<ReEventEntity> mockReEventEntities = List.of();

        when(reRepository.findSessionWithoutRedirect(anyString(), anyString())).thenReturn(mockSessionEntities);
        when(reRepository.findBySessionIdAndStatus(anyString(), anyString(), anyString(), anyString(), anyInt())).thenReturn(mockReEventEntities);

        // Act
        int recoveredReceipt = recoveryService.recoverMissingRedirect(dateFrom, dateTo);

        // Assert
        assertEquals(0, recoveredReceipt);
    }

    @Test
    void testRecoverReceiptKOByCI_Success_Empty() {
        // Arrange
        String creditorInstitution = "77777777777";
        String dateFrom = "2024-09-05";
        String dateTo = "2024-09-09";
        List<RTEntity> mockRTEntities = List.of();

        when(rtRepository.findByMidReceiptStatusInAndTimestampBetween(anyString(), anyString(), anyString())).thenReturn(mockRTEntities);

        // Act
        RecoveryReceiptResponse response = recoveryService.recoverReceiptKOByCI(creditorInstitution, dateFrom, dateTo);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getPayments().size());
    }

    @Test
    void testRecoverReceiptKOByCI_Success_NotEmpty() {
        // Arrange
        String creditorInstitution = "77777777777";
        String dateFrom = "2024-09-05";
        String dateTo = "2024-09-09";
        List<RTEntity> mockRTEntities = List.of(RTEntity.builder()
                .domainId("ci")
                .iuv("iuv")
                .ccp("ccp")
                .build());

        when(rtRepository.findByMidReceiptStatusInAndTimestampBetween(anyString(), anyString(), anyString())).thenReturn(mockRTEntities);
        doNothing().when(receiptService)
                .sendRTKoFromSessionId(anyString(), any());
        doNothing().when(reService).addRe(any(ReEventDto.class));

        // Act
        RecoveryReceiptResponse response = recoveryService.recoverReceiptKOByCI(creditorInstitution, dateFrom, dateTo);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getPayments().size());
    }

    @Test
    void testCallSendReceiptKOByCI_Success_Today() {
        // Arrange
        String creditorInstitution = "77777777777";
        String dateFrom = "2024-09-05";
        String dateTo = LocalDate.now().toString();
        List<RTEntity> mockRTEntities = List.of();

        when(rtRepository.findByMidReceiptStatusInAndTimestampBetween(anyString(), anyString(), anyString())).thenReturn(mockRTEntities);

        // Act
        RecoveryReceiptResponse response = recoveryService.recoverReceiptKOByCI(creditorInstitution, dateFrom, dateTo);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getPayments().size());
    }

    @Test
    void testCallSendReceiptKOByCI_LowerBoundFailure() {
        // Arrange
        String creditorInstitution = "77777777777";
        String dateFrom = "2024-09-01";  // Date earlier than valid start date
        String dateTo = "2024-09-09";

        // Act and Assert
        AppException exception = assertThrows(
                AppException.class, () -> recoveryService.recoverReceiptKOByCI(creditorInstitution, dateFrom, dateTo)
        );
        assertEquals("The lower bound cannot be lower than [2024-09-03]", exception.getMessage());
    }

    @Test
    void testCallSendReceiptKOByCI_UpperBoundFailure() {
        // Arrange
        String creditorInstitution = "77777777777";
        String dateFrom = "2024-09-05";
        String dateTo = LocalDate.now().plusDays(1).toString();  // Future date, should fail

        // Act and Assert
        AppException exception = assertThrows(
                AppException.class, () -> recoveryService.recoverReceiptKOByCI(creditorInstitution, dateFrom, dateTo)
        );
        String expectedMessage = String.format("The upper bound cannot be higher than [%s]", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void testCallSendReceiptKOByIUV() {
        // Arrange
        String dateFrom = "2024-09-05";
        String dateTo = "2024-09-09";
        String ci = "ci";
        String iuv = "iuv";

        when(rtRepository.findByMidReceiptStatusInAndTimestampBetween(anyString(), anyString(), anyString(), anyString())).thenReturn(List.of());
        doNothing().when(receiptService).sendRTKoFromSessionId(anyString(), any());
        doNothing().when(reService).addRe(any(ReEventDto.class));

        // Act
        RecoveryReceiptResponse response = recoveryService.recoverReceiptKOByIUV(ci, iuv, dateFrom, dateTo);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getPayments().size());
        verify(receiptService, times(0)).sendRTKoFromSessionId(anyString(), any(InternalStepStatus.class));
    }

    @Test
    void testCallSendReceiptKOByIUV_NotEmpty() {
        // Arrange
        String iuv = "00000000000000000";
        String creditorInstitution = "77777777777";
        String dateFrom = "2024-09-05";
        String dateTo = "2024-09-09";
        List<RTEntity> mockRTEntities = List.of(RTEntity.builder()
                                                        .domainId("ci")
                                                        .iuv("iuv")
                                                        .ccp("ccp")
                                                        .sessionId("sessionId")
                                                        .build());

        when(rtRepository.findByMidReceiptStatusInAndTimestampBetween(anyString(), anyString(), anyString(), anyString())).thenReturn(mockRTEntities);

        // Act
        RecoveryReceiptResponse response = recoveryService.recoverReceiptKOByIUV(creditorInstitution, iuv, dateFrom, dateTo);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getPayments().size());
        verify(receiptService, times(1)).sendRTKoFromSessionId(anyString(), any(InternalStepStatus.class));
    }
}