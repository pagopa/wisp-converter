package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.controller.ReceiptController;
import it.gov.pagopa.wispconverter.controller.model.RecoveryReceiptResponse;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.repository.RTRepository;
import it.gov.pagopa.wispconverter.repository.ReEventRepository;
import it.gov.pagopa.wispconverter.repository.model.RTEntity;
import it.gov.pagopa.wispconverter.repository.model.ReEventEntity;
import it.gov.pagopa.wispconverter.repository.model.SessionIdEntity;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RecoveryServiceTest {

    @Mock
    private RTRepository rtRepository;

    @Mock
    private ReEventRepository reRepository;

    @Mock
    private CacheRepository cacheRepository;

    @Mock
    private ReceiptController receiptController;

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
    public void testRecoverReceiptKO() throws Exception {
        // Arrange
        String dateFrom = "2024-09-05";
        String dateTo = "2024-09-09";
        String ci = "ci";
        String nav = "nav";
        String iuv = "iuv";
        String ccp = "cpp";
        String session = "sessionId";
        List<ReEventEntity> rtSuccessReEventEntity = List.of();

        when(reRepository.findBySessionIdAndStatus(anyString(), anyString(), anyString(), anyString())).thenReturn(rtSuccessReEventEntity);
        doNothing().when(cacheRepository)
                .insert(anyString(), anyString(), anyLong(), any(ChronoUnit.class), anyBoolean());
        doNothing().when(receiptController)
                           .receiptKo(anyString());
        doNothing().when(reService).addRe(any(ReEventDto.class));

        // Act
        recoveryService.recoverReceiptKO(ci, nav, iuv, session, ccp, dateFrom, dateTo);

        // Assert
        verify(cacheRepository, times(2)).insert(anyString(), anyString(), anyLong(), any(ChronoUnit.class), anyBoolean());
    }

    @Test
    public void testRecoverReceiptKOAll() {
        // Arrange
        ZonedDateTime dateFrom = ZonedDateTime.now(ZoneOffset.UTC).minusHours(5);
        ZonedDateTime dateTo = ZonedDateTime.now(ZoneOffset.UTC).minusHours(4);
        List<RTEntity> mockRTEntities = List.of();

        when(rtRepository.findPendingRT(anyString(), anyString())).thenReturn(mockRTEntities);

        // Act
        int recoveredReceipt = recoveryService.recoverReceiptKOAll(dateFrom, dateTo);

        // Assert
        assertEquals(0, recoveredReceipt);
    }

    @Test
    public void testRecoverReceiptKOAll_notEmpty() {
        // Arrange
        ZonedDateTime dateFrom = ZonedDateTime.now(ZoneOffset.UTC).minusHours(5);
        ZonedDateTime dateTo = ZonedDateTime.now(ZoneOffset.UTC).minusHours(4);
        List<RTEntity> mockRTEntities = List.of(RTEntity.builder()
                                                        .iuv("iuv")
                                                        .ccp("ccp")
                                                        .idDominio("idDominio")
                                                        .build());
        List<ReEventEntity> mockReEventEntities = List.of(ReEventEntity.builder()
                                                                  .status("GENERATED_CACHE_ABOUT_RPT_FOR_RT_GENERATION")
                                                                  .ccp("ccp2")
                                                                  .insertedTimestamp(Instant.now())
                                                                  .build());

        when(rtRepository.findPendingRT(anyString(), anyString())).thenReturn(mockRTEntities);
        when(reRepository.findByIuvAndOrganizationId(anyString(), anyString(), anyString(), anyString())).thenReturn(mockReEventEntities);

        // Act
        int recoveredReceipt = recoveryService.recoverReceiptKOAll(dateFrom, dateTo);

        // Assert
        assertEquals(1, recoveredReceipt);
    }

    @Test
    public void testRecoverMissingRedirect() {
        // Arrange
        ZonedDateTime dateFrom = ZonedDateTime.now(ZoneOffset.UTC).minusHours(5);
        ZonedDateTime dateTo = ZonedDateTime.now(ZoneOffset.UTC).minusHours(4);
        List<SessionIdEntity> mockSessionEntities = List.of();
        List<ReEventEntity> mockReEventEntities = List.of();

        when(reRepository.findSessionWithoutRedirect(anyString(), anyString())).thenReturn(mockSessionEntities);
        when(reRepository.findBySessionIdAndStatus(anyString(), anyString(), anyString(), anyString())).thenReturn(mockReEventEntities);

        // Act
        int recoveredReceipt = recoveryService.recoverMissingRedirect(dateFrom, dateTo);

        // Assert
        assertEquals(0, recoveredReceipt);
    }

    @Test
    public void testRecoverMissingRedirect_notEmpty() {
        // Arrange
        ZonedDateTime dateFrom = ZonedDateTime.now(ZoneOffset.UTC).minusHours(5);
        ZonedDateTime dateTo = ZonedDateTime.now(ZoneOffset.UTC).minusHours(4);
        List<SessionIdEntity> mockSessionEntities = List.of(SessionIdEntity.builder().sessionId("mockSessionId").build());
        List<ReEventEntity> mockReEventEntities = List.of();

        when(reRepository.findSessionWithoutRedirect(anyString(), anyString())).thenReturn(mockSessionEntities);
        when(reRepository.findBySessionIdAndStatus(anyString(), anyString(), anyString(), anyString())).thenReturn(mockReEventEntities);

        // Act
        int recoveredReceipt = recoveryService.recoverMissingRedirect(dateFrom, dateTo);

        // Assert
        assertEquals(1, recoveredReceipt);
    }

    @Test
    public void testRecoverReceiptKOForCreditorInstitution_Success() {
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
    public void testRecoverReceiptKOForCreditorInstitution_LowerBoundFailure() {
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
    public void testRecoverReceiptKOForCreditorInstitution_UpperBoundFailure() {
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
}