package it.gov.pagopa.wispconverter.service;

import com.azure.cosmos.CosmosException;
import com.azure.spring.data.cosmos.exception.CosmosAccessException;
import it.gov.pagopa.wispconverter.entity.RPTRequestEntity;
import it.gov.pagopa.wispconverter.exception.conversion.ConversionException;
import it.gov.pagopa.wispconverter.model.converter.ConversionResult;
import it.gov.pagopa.wispconverter.model.unmarshall.RPTRequest;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import it.gov.pagopa.wispconverter.util.FileReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ConverterService {

    private final NAVGeneratorService navGeneratorService;

    private final UnmarshallerService unmarshallerService;

    private final RPTRequestRepository rptRequestRepository;

    private final CacheRepository cacheRepository;

    private final FileReader fileReader;

    public ConverterService(@Autowired NAVGeneratorService navGeneratorService,
                            @Autowired UnmarshallerService unmarshallerService,
                            @Autowired RPTRequestRepository rptRequestRepository,
                            @Autowired CacheRepository cacheRepository,
                            @Autowired FileReader fileReader) {
        this.navGeneratorService = navGeneratorService;
        this.unmarshallerService = unmarshallerService;
        this.rptRequestRepository = rptRequestRepository;
        this.cacheRepository = cacheRepository;
        this.fileReader = fileReader;
    }

    @SuppressWarnings({"rawtypes"})
    public ConversionResult convert(String sessionId) {

        ConversionResult conversionResult = null;
        try {
            // get request from CosmosDB
            RPTRequestEntity rptRequestEntity = getRPTRequestEntity(sessionId);

            // parse header and body from request entity
            RPTRequest rptRequest = this.unmarshallerService.unmarshall(rptRequestEntity);

            // for each RPT in list
            // call IUV Generator API for generate NAV
            List<byte[]> rawRPTs = CommonUtility.getAllRawRPTs(rptRequest);
            String creditorInstitutionCode = CommonUtility.getCreditorInstitutionCode(rptRequest);
            String navCode = this.navGeneratorService.getNAVCodeFromIUVGenerator(creditorInstitutionCode);

            // execute mapping of COSMOSDB data for GPD insert
            this.unmarshallerService.unmarshall(rptRequestEntity);

            // call GPD bulk creation API

            // call APIM policy for save key for decoupler

            // save key in REDIS for 'primitiva di innesco'

            // execute mapping for Checkout carts invocation

            // call Checkout carts API

            // receive Checkout response and returns redirection URI
        } catch (ConversionException e) {
            log.error("Error while executing RPT conversion. ", e);
            conversionResult = getHTMLErrorPage();
        }


        return conversionResult;
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
