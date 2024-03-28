package it.gov.pagopa.wispconverter.service;

import com.azure.cosmos.CosmosException;
import com.azure.spring.data.cosmos.exception.CosmosAccessException;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.service.model.ConversionResultDTO;
import it.gov.pagopa.wispconverter.service.model.RPTContentDTO;
import it.gov.pagopa.wispconverter.util.FileReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConverterService {

    private final RPTExtractorService rptExtractorService;

    private final DebtPositionService debtPositionService;

    private final CacheService cacheService;

    private final CheckoutService checkoutService;

    private final RPTRequestRepository rptRequestRepository;

    private final FileReader fileReader;


    public ConversionResultDTO convert(String sessionId) {

        ConversionResultDTO conversionResultDTO = null;
        try {
            // get RPT request entity from database
            RPTRequestEntity rptRequestEntity = getRPTRequestEntity(sessionId);

            // unmarshalling and mapping RPT content from request entity
            List<RPTContentDTO> rptContentDTOs = this.rptExtractorService.extractRPTContentDTOs(rptRequestEntity.getPrimitive(), rptRequestEntity.getPayload());

            // calling GPD creation API in order to generate the debt position associated to RPTs
            this.debtPositionService.createDebtPositions(rptContentDTOs);

            // call APIM policy for save key for decoupler and save in Redis cache the mapping of the request identifier needed for RT generation in next steps
            this.cacheService.storeRequestMappingInCache(rptContentDTOs, sessionId);

            // execute communication with Checkout service and set the redirection URI as response
            String redirectURI = this.checkoutService.executeCall();
            conversionResultDTO.setUri(redirectURI);

        } catch (IOException e) {
            log.error("Error while executing RPT conversion. ", e);
            conversionResultDTO = getHTMLErrorPage();
        }

        return conversionResultDTO;
    }

    private RPTRequestEntity getRPTRequestEntity(String sessionId) {
        try {
            Optional<RPTRequestEntity> optRPTReqEntity = this.rptRequestRepository.findById(sessionId);
            return optRPTReqEntity.orElseThrow(() -> new AppException(AppErrorCodeMessageEnum.PERSISTENCE_));
        } catch (CosmosException | CosmosAccessException e) {
            throw new AppException(AppErrorCodeMessageEnum.PERSISTENCE_);
        }
        // TODO RE
    }

    private ConversionResultDTO getHTMLErrorPage() {
        ConversionResultDTO conversionResultDTO = ConversionResultDTO.builder().build();
        try {
            conversionResultDTO.setErrorPage(this.fileReader.readFileFromResources("static/error.html"));
        } catch (IOException e) {
            conversionResultDTO.setErrorPage("<!DOCTYPE html><html lang=\"en\"><head></head><body>No content found</body></html>");
        }
        return conversionResultDTO;
    }
}
