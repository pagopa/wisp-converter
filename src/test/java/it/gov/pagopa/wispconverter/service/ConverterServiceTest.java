package it.gov.pagopa.wispconverter.service;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import it.gov.pagopa.gen.wispconverter.client.checkout.model.CartRequestDto;
import it.gov.pagopa.gen.wispconverter.client.checkout.model.PaymentNoticeDto;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.service.model.session.CommonFieldsDTO;
import it.gov.pagopa.wispconverter.service.model.session.PaymentNoticeContentDTO;
import it.gov.pagopa.wispconverter.service.model.session.SessionDataDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConverterServiceTest {

	@Spy
	@InjectMocks
	private ConverterService converterService;

	@Mock
	private RPTExtractorService rptExtractorService;

	@Mock
	private DebtPositionService debtPositionService;

	@Mock
	private DecouplerService decouplerService;

	@Mock
	private CheckoutService checkoutService;

	@Mock
	private RptCosmosService rptCosmosService;

	@Mock
	private RtReceiptCosmosService rtReceiptCosmosService;

	@Mock
	private ReceiptService receiptService;

	@Mock
	private ServiceBusSenderClient serviceBusSenderClient;

	@Mock
	private ReService reService;

	@Mock
	private CacheRepository cacheRepository;

	@Mock
	private ECommerceHangTimerService eCommerceHangTimerService;

	@Mock
	private RPTTimerService rptTimerService;

	@Captor
	ArgumentCaptor<ServiceBusMessage> messageArgumentCaptor;

	private final SessionDataDTO sessionDataDTO = ConverterServiceTest.getMockSessionDataDTO();

	@BeforeEach
	void setUp() {
		when(rptCosmosService.getRPTRequestEntity(anyString())).thenReturn(RPTRequestEntity.builder()
				.id("mock-id")
				.primitive("nodoInviaRPT")
				.payload("mock-payload")
				.build());
		when(rptExtractorService.extractSessionData(anyString(), anyString())).thenReturn(sessionDataDTO); 
	}

	@Test
	void convert_OK() throws URISyntaxException {
		// precondition
		when(checkoutService.executeCall(sessionDataDTO)).thenReturn("mock-checkout-redirect-url");
		PaymentNoticeDto paymentNoticeDto = new PaymentNoticeDto();
		paymentNoticeDto.setFiscalCode("mock-fiscal-code");
		paymentNoticeDto.setNoticeNumber("mock-cart-nav");  
		CartRequestDto cartRequestDto = new CartRequestDto();
		cartRequestDto.setPaymentNotices(List.of(paymentNoticeDto));
		when(checkoutService.extractCart(sessionDataDTO)).thenReturn(cartRequestDto);

		String checkoutResponse = converterService.convert(UUID.randomUUID().toString());
		Assertions.assertEquals("mock-checkout-redirect-url", checkoutResponse);
	}

	@Test
	void convert_Exception_KO() throws URISyntaxException {
		// precondition
		doThrow(new OptimisticLockingFailureException("mock-exception")).when(debtPositionService).createDebtPositions(sessionDataDTO);
		try {
			converterService.convert(UUID.randomUUID().toString());
		} catch (Exception e) {
			if (e.getMessage().equals("org.springframework.dao.OptimisticLockingFailureException: mock-exception")) {
				verify(receiptService, times(1)).sendRTKoFromSessionId(anyString());
			} else {
				Assertions.fail();
			}
		}
	}
	
	@Test
	void convert_AppException_KO() throws URISyntaxException {
		// precondition
		doThrow(new AppException(AppErrorCodeMessageEnum.GENERIC_ERROR)).when(debtPositionService).createDebtPositions(sessionDataDTO);
		try {
			converterService.convert(UUID.randomUUID().toString());
		} catch (AppException e) {
			if (e.getMessage().contains(AppErrorCodeMessageEnum.GENERIC_ERROR.getDetail())) {
				verify(receiptService, times(1)).sendRTKoFromSessionId(anyString());
			} else {
				Assertions.fail();
			}
		} 
		catch (Exception e) {
			Assertions.fail();
		}
	}

	private static SessionDataDTO getMockSessionDataDTO() {
		Map<String, PaymentNoticeContentDTO> paymentNotices = new HashMap<>(); 
		paymentNotices.put("mock-iuv", PaymentNoticeContentDTO.builder()
				.noticeNumber("mock-nav")
				.build());
		return SessionDataDTO.builder()
				.commonFields(CommonFieldsDTO.builder().creditorInstitutionId("mock-creditorInstitutionId").build())
				.paymentNotices(paymentNotices)
				.build();
	}


}