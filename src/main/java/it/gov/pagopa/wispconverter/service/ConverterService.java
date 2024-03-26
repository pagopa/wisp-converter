package it.gov.pagopa.wispconverter.service;

import com.azure.cosmos.CosmosException;
import com.azure.spring.data.cosmos.exception.CosmosAccessException;
import it.gov.pagopa.wispconverter.entity.RPTRequestEntity;
import it.gov.pagopa.wispconverter.exception.conversion.ConversionException;
import it.gov.pagopa.wispconverter.model.client.gpd.MultiplePaymentPosition;
import it.gov.pagopa.wispconverter.model.client.gpd.PaymentPosition;
import it.gov.pagopa.wispconverter.model.converter.ConversionResult;
import it.gov.pagopa.wispconverter.model.unmarshall.RPTContent;
import it.gov.pagopa.wispconverter.model.unmarshall.RPTRequest;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import it.gov.pagopa.wispconverter.util.FileReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ConverterService {

    private final NAVGeneratorService navGeneratorService;

    private final DebtPositionService debtPositionService;

    private final CacheService cacheService;

    private final CheckoutService checkoutService;

    private final UnmarshallerService unmarshallerService;

    private final RPTRequestRepository rptRequestRepository;

    private final FileReader fileReader;

    public ConverterService(@Autowired NAVGeneratorService navGeneratorService,
                            @Autowired DebtPositionService debtPositionService,
                            @Autowired CacheService cacheService,
                            @Autowired CheckoutService checkoutService,
                            @Autowired UnmarshallerService unmarshallerService,
                            @Autowired RPTRequestRepository rptRequestRepository,
                            @Autowired FileReader fileReader) {
        this.navGeneratorService = navGeneratorService;
        this.debtPositionService = debtPositionService;
        this.cacheService = cacheService;
        this.checkoutService = checkoutService;
        this.unmarshallerService = unmarshallerService;
        this.rptRequestRepository = rptRequestRepository;
        this.fileReader = fileReader;
    }

    @SuppressWarnings({"rawtypes"})
    public ConversionResult convert(String sessionId) {

        ConversionResult conversionResult = null;
        try {
            // get request entity from CosmosDB
            RPTRequestEntity rptRequestEntity = getRPTRequestEntity(sessionId);

            // unmarshalling header and body from request entity
            RPTRequest rptRequest = this.unmarshallerService.unmarshall(rptRequestEntity);

            // for each RPT in list
            List<PaymentPosition> paymentPositions = new ArrayList<>();
            for (RPTContent rptContent : CommonUtility.getAllRawRPTs(rptRequest)) {

                // parse single RPTs from Base64, unmarshalling from extracted XML string
                rptContent.setRpt(this.unmarshallerService.unmarshall(rptContent));

                // execute mapping of parsed RPT to debt position to be sent to GPD service
                PaymentPosition paymentPosition = mapRPTToDebtPosition(rptRequest, rptContent); // TODO implement this method

                // include new mapped debt position in the list
                paymentPositions.add(paymentPosition);
            }

            // extracting creditor institution code from header and call GPD bulk creation API
            String creditorInstitutionCode = CommonUtility.getCreditorInstitutionCode(rptRequest);
            MultiplePaymentPosition multiplePaymentPosition = this.debtPositionService.executeBulkCreation(creditorInstitutionCode, paymentPositions);

            // call APIM policy for save key for decoupler and save in Redis cache the mapping of the request identifier needed for RT generation in next steps
            this.cacheService.storeRequestMappingInCache(creditorInstitutionCode, multiplePaymentPosition, sessionId);

            // execute communication with Checkout service and set the redirection URI as response
            URI redirectURI = this.checkoutService.executeCall();
            conversionResult.setUri(redirectURI);

        } catch (ConversionException e) {
            log.error("Error while executing RPT conversion. ", e);
            conversionResult = getHTMLErrorPage();
        }

        return conversionResult;
    }

    @SuppressWarnings({"rawtypes"})
    private PaymentPosition mapRPTToDebtPosition(RPTRequest rptRequest, RPTContent rptContent) {

        // call IUV Generator API for generate NAV
        String creditorInstitutionCode = rptContent.getRpt().getDominio().getIdentificativoDominio();
        String navCode = this.navGeneratorService.getNAVCodeFromIUVGenerator(creditorInstitutionCode);

        // TODO mapping
    }

    private RPTRequestEntity getRPTRequestEntity(String sessionId) throws ConversionException {
        try {
            Optional<RPTRequestEntity> optRPTReqEntity = this.rptRequestRepository.findById(sessionId);
            return optRPTReqEntity.orElseThrow(() -> new ConversionException(String.format("Unable to retrieve RPT request from CosmosDB storage. No valid element found with id [%s].", sessionId)));
        } catch (CosmosException | CosmosAccessException e) {
            throw new ConversionException("Unable to retrieve RPT request from CosmosDB storage. An exception occurred while reading from storage:", e);
        }
    }

    private ConversionResult getHTMLErrorPage() {
        ConversionResult conversionResult = ConversionResult.builder().build();
        try {
            conversionResult.setErrorPage(this.fileReader.readFileFromResources("static/error.html"));
        } catch (IOException e) {
            conversionResult.setErrorPage("<!DOCTYPE html><html lang=\"en\"><head></head><body>No content found</body></html>");
        }
        return conversionResult;
    }
}
