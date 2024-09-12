package it.gov.pagopa.wispconverter.service;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

import it.gov.pagopa.gen.wispconverter.client.checkout.model.CartRequestDto;
import it.gov.pagopa.gen.wispconverter.client.checkout.model.PaymentNoticeDto;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.service.model.ReceiptDto;
import it.gov.pagopa.wispconverter.service.model.session.CommonFieldsDTO;
import it.gov.pagopa.wispconverter.service.model.session.PaymentNoticeContentDTO;
import it.gov.pagopa.wispconverter.service.model.session.SessionDataDTO;

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
		Assert.assertEquals("mock-checkout-redirect-url", checkoutResponse);
	}

	@Test
	void convert_Exception_KO() throws URISyntaxException {
		// precondition
		doThrow(new OptimisticLockingFailureException("mock-exception")).when(debtPositionService).createDebtPositions(sessionDataDTO);
		try {
			converterService.convert(UUID.randomUUID().toString());
		} catch (Exception e) {
			if (e.getMessage().equals("mock-exception")) {
				var inputPaaInviaRTKo = List.of(ReceiptDto.builder()
						.fiscalCode(sessionDataDTO.getCommonFields().getCreditorInstitutionId())
						.noticeNumber(sessionDataDTO.getNAVs().get(0))
						.build());
				verify(receiptService, times(1)).sendKoPaaInviaRtToCreditorInstitution(inputPaaInviaRTKo);
			} else {
				fail();
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
			if (e.getMessage().equals(AppErrorCodeMessageEnum.GENERIC_ERROR.getDetail())) {
				var inputPaaInviaRTKo = List.of(ReceiptDto.builder()
						.fiscalCode(sessionDataDTO.getCommonFields().getCreditorInstitutionId())
						.noticeNumber(sessionDataDTO.getNAVs().get(0))
						.build());
				verify(receiptService, times(1)).sendKoPaaInviaRtToCreditorInstitution(inputPaaInviaRTKo);
			} else {
				fail();
			}
		} 
		catch (Exception e) {
			fail();
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