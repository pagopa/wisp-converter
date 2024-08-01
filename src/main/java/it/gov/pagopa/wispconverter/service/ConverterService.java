package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.service.model.session.SessionDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    public String convert(String sessionId) {
        // get RPT request entity from database
        RPTRequestEntity rptRequestEntity = rptCosmosService.getRPTRequestEntity(sessionId);

        // unmarshalling and mapping RPT content from request entity, generating session data
        SessionDataDTO sessionData = this.rptExtractorService.extractSessionData(rptRequestEntity.getPrimitive(), rptRequestEntity.getPayload());

        // prepare receipt-rt saving (nodoChiediCopiaRT)
        rtReceiptCosmosService.saveRTEntity(sessionData);

        // calling GPD creation API in order to generate the debt position associated to RPTs
        this.debtPositionService.createDebtPositions(sessionData);

        // call APIM policy for save key for decoupler and save in Redis cache the mapping of the request identifier needed for RT generation in next steps
        this.decouplerService.storeRequestMappingInCache(sessionData, sessionId);

        // execute communication with Checkout service and set the redirection URI as response
        return this.checkoutService.executeCall(sessionData);
    }
}
