package it.gov.pagopa.wispconverter.service;

import java.net.URISyntaxException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import it.gov.pagopa.gen.wispconverter.client.checkout.model.CartRequestDto;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.service.model.ECommerceHangTimeoutMessage;
import it.gov.pagopa.wispconverter.service.model.ReceiptDto;
import it.gov.pagopa.wispconverter.service.model.session.SessionDataDTO;
import it.gov.pagopa.wispconverter.servicebus.ECommerceHangTimeoutConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
@RequiredArgsConstructor
public class ConverterService {

    private final RPTExtractorService rptExtractorService;

    private final DebtPositionService debtPositionService;

    private final DecouplerService decouplerService;

    private final CheckoutService checkoutService;

    private final RptCosmosService rptCosmosService;

    private final RtReceiptCosmosService rtReceiptCosmosService;

    private final ECommerceHangTimerService eCommerceHangTimerService;

    private final ReceiptService receiptService;

    public String convert(String sessionId) throws URISyntaxException {
    	String checkoutResponse;
    	SessionDataDTO sessionData = null;
    	try {
    		// get RPT request entity from database
    		RPTRequestEntity rptRequestEntity = rptCosmosService.getRPTRequestEntity(sessionId);

    		// unmarshalling and mapping RPT content from request entity, generating session data
    		sessionData = this.rptExtractorService.extractSessionData(rptRequestEntity.getPrimitive(), rptRequestEntity.getPayload());

    		// prepare receipt-rt saving (nodoChiediCopiaRT)
    		rtReceiptCosmosService.saveRTEntity(sessionData);

    		// calling GPD creation API in order to generate the debt position associated to RPTs
    		this.debtPositionService.createDebtPositions(sessionData);

    		// call APIM policy for save key for decoupler and save in Redis cache the mapping of the request identifier needed for RT generation in next steps
    		this.decouplerService.storeRequestMappingInCache(sessionData, sessionId);

    		// execute communication with Checkout service and set the redirection URI as response
    		checkoutResponse = this.checkoutService.executeCall(sessionData);

    		// call APIM policy for save key for cart session handling
    		this.decouplerService.storeRequestCartMappingInCache(sessionData, sessionId);

    		// set eCommerce timer foreach notices in the cart
    		setECommerceHangTimer(sessionData);
    		
    	} catch (AppException appException) {
    		log.error("An error occurred during covert operations: " + appException.getMessage());
    		// TODO decide what to do if it is not possible to recover the sessionData obj or if the information necessary to valorise the sendKoPaaInviaRtToCreditorInstitution is not present.
    		if (sessionData != null && sessionData.getCommonFields() != null && !CollectionUtils.isEmpty(sessionData.getNAVs())) {
    			List<String> navList = sessionData.getNAVs();
    			for (String noticeNumber: navList) {
    				var inputPaaInviaRTKo = List.of(ReceiptDto.builder()
    	                    .fiscalCode(sessionData.getCommonFields().getCreditorInstitutionId())
    	                    .noticeNumber(noticeNumber)
    	                    .build());
    	            receiptService.sendKoPaaInviaRtToCreditorInstitution(inputPaaInviaRTKo);
    			}
    		}
    		throw appException;   		
    	} catch (Exception ex) {
    		log.error("An error occurred during covert operations: " + ex.getMessage());
    		// TODO decide what to do if it is not possible to recover the sessionData obj or if the information necessary to valorise the sendKoPaaInviaRtToCreditorInstitution is not present.
    		if (sessionData != null && sessionData.getCommonFields() != null && !CollectionUtils.isEmpty(sessionData.getNAVs())) {
    			List<String> navList = sessionData.getNAVs();
    			for (String noticeNumber: navList) {
    				var inputPaaInviaRTKo = List.of(ReceiptDto.builder()
    	                    .fiscalCode(sessionData.getCommonFields().getCreditorInstitutionId())
    	                    .noticeNumber(noticeNumber)
    	                    .build());
    	            receiptService.sendKoPaaInviaRtToCreditorInstitution(inputPaaInviaRTKo);
    			}
    		}
    		throw ex;
    	}
    	return checkoutResponse;
    }

    /**
     * This method inserts a scheduled message in the queue of the service bus.
     * When the message is trigger a sendRT-Negative is sent to the Creditor Institution
     * (see {@link ECommerceHangTimeoutConsumer} class for more details).
     *
     * @param sessionData Data of the cart with the paymentOptions
     * @throws URISyntaxException
     */
    private void setECommerceHangTimer(SessionDataDTO sessionData) throws URISyntaxException {
        CartRequestDto cart = checkoutService.extractCart(sessionData);
        cart.getPaymentNotices().forEach(elem ->
                eCommerceHangTimerService.sendMessage(ECommerceHangTimeoutMessage.builder()
                        .fiscalCode(elem.getFiscalCode())
                        .noticeNumber(elem.getNoticeNumber())
                        .build()));
    }
}
