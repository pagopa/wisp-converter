package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.gen.wispconverter.client.checkout.model.CartRequestDto;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.repository.model.enumz.ReceiptStatusEnum;
import it.gov.pagopa.wispconverter.repository.model.enumz.WorkflowStatus;
import it.gov.pagopa.wispconverter.service.model.ECommerceHangTimeoutMessage;
import it.gov.pagopa.wispconverter.service.model.session.SessionDataDTO;
import it.gov.pagopa.wispconverter.servicebus.ECommerceHangTimeoutConsumer;
import it.gov.pagopa.wispconverter.servicebus.RPTTimeoutConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;


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

    private final RPTTimerService rptTimerService;

    private final ReceiptService receiptService;

    private final ReService reService;

    public String convert(String sessionId) {

        // deleting timer on RPT
        removeRPTTimer(sessionId);

        // get RPT request entity from database
        RPTRequestEntity rptRequestEntity = rptCosmosService.getRPTRequestEntity(sessionId);
        String checkoutResponse;
        try {
            // unmarshalling and mapping RPT content from request entity, generating session data
            SessionDataDTO sessionData = this.rptExtractorService.extractSessionData(rptRequestEntity.getPrimitive(), rptRequestEntity.getPayload());
            this.rptExtractorService.sendEventForExtractedRPTs(sessionData.getAllRPTs());

            // prepare receipt-rt saving (nodoChiediCopiaRT)
            rtReceiptCosmosService.saveRTEntity(sessionData, ReceiptStatusEnum.REDIRECT);

            // calling GPD creation API in order to generate the debt position associated to RPTs
            this.debtPositionService.createDebtPositions(sessionData);

            // call APIM policy for save key for decoupler and save in Redis cache the mapping of the request identifier needed for RT generation in next steps
            this.decouplerService.storeRequestMappingInCache(sessionData);

            // execute communication with Checkout service and set the redirection URI as response
            checkoutResponse = this.checkoutService.executeCall(sessionData);

            // call APIM policy for save key for cart session handling
            this.decouplerService.storeRequestCartMappingInCache(sessionData, sessionId);

            // set eCommerce timer foreach notices in the cart
            setECommerceHangTimer(sessionData);

        } catch (Exception ex) {
            log.debug("A generic error occurred during convert operations: {}", ex.getMessage());
            receiptService.sendRTKoFromSessionId(sessionId);
            if (ex instanceof AppException e) {
                throw e;
            } else {
                throw new AppException(AppErrorCodeMessageEnum.ERROR, ex);
            }
        }
        return checkoutResponse;
    }

    /**
     * This method inserts a scheduled message in the queue of the service bus.
     * When the message is trigger a sendRT-Negative is sent to the Creditor Institution
     * (see {@link ECommerceHangTimeoutConsumer} class for more details).
     *
     * @param sessionData Data of the cart with the paymentOptions
     */
    private void setECommerceHangTimer(SessionDataDTO sessionData) {
        CartRequestDto cart = null;
        try {
            cart = checkoutService.extractCart(sessionData);
        } catch (URISyntaxException e) {
            throw new AppException(AppErrorCodeMessageEnum.ERROR, e);
        }
        String sessionId = sessionData.getCommonFields().getSessionId();
        cart.getPaymentNotices().forEach(elem ->
                eCommerceHangTimerService.sendMessage(ECommerceHangTimeoutMessage.builder()
                        .fiscalCode(elem.getFiscalCode())
                        .noticeNumber(elem.getNoticeNumber())
                        .sessionId(sessionId)
                        .build()));
    }

    /**
     * This method inserts a scheduled message in the queue of the service bus.
     * When the message is trigger a sendRT-Negative is sent to the Creditor Institution
     * (see {@link RPTTimeoutConsumer} class for more details).
     *
     * @param sessionId Data of the cart with the paymentOptions
     */
    private void removeRPTTimer(String sessionId) {
        rptTimerService.cancelScheduledMessage(sessionId);
        reService.sendEvent(WorkflowStatus.RPT_TIMER_DELETED);
    }

}
