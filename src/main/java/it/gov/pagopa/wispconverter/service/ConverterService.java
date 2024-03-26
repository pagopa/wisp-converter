package it.gov.pagopa.wispconverter.service;

import com.azure.cosmos.CosmosException;
import com.azure.spring.data.cosmos.exception.CosmosAccessException;
import gov.telematici.pagamenti.ws.NodoInviaCarrelloRPT;
import gov.telematici.pagamenti.ws.NodoInviaRPT;
import gov.telematici.pagamenti.ws.TipoElementoListaRPT;
import gov.telematici.pagamenti.ws.ppthead.IntestazioneCarrelloPPT;
import gov.telematici.pagamenti.ws.ppthead.IntestazionePPT;
import it.gov.digitpa.schemas._2011.pagamenti.CtRichiestaPagamentoTelematico;
import it.gov.pagopa.wispconverter.entity.Primitive;
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
import it.gov.pagopa.wispconverter.util.JaxbElementUtil;
import it.gov.pagopa.wispconverter.util.ZipUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.xmlsoap.schemas.soap.envelope.Envelope;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ConverterService {

    private final NAVGeneratorService navGeneratorService;

    private final DebtPositionService debtPositionService;

    private final CacheService cacheService;

    private final CheckoutService checkoutService;

    private final RPTRequestRepository rptRequestRepository;

    private final FileReader fileReader;

    private final JaxbElementUtil jaxbElementUtil;

    public ConverterService(@Autowired NAVGeneratorService navGeneratorService,
                            @Autowired DebtPositionService debtPositionService,
                            @Autowired CacheService cacheService,
                            @Autowired CheckoutService checkoutService,
                            @Autowired JaxbElementUtil jaxbElementUtil,
                            @Autowired RPTRequestRepository rptRequestRepository,
                            @Autowired FileReader fileReader) {
        this.navGeneratorService = navGeneratorService;
        this.debtPositionService = debtPositionService;
        this.cacheService = cacheService;
        this.checkoutService = checkoutService;
        this.jaxbElementUtil = jaxbElementUtil;
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
            RPTRequest rptRequest = extractRPTRequest(rptRequestEntity);

            // for each RPT in list
            List<PaymentPosition> paymentPositions = new ArrayList<>();
            for (RPTContent rptContent : CommonUtility.getAllRawRPTs(rptRequest)) {

                // parse single RPTs from Base64, unmarshalling from extracted XML string
                rptContent.setRpt(extractRPT(rptContent));

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
    private RPTRequest extractRPTRequest(RPTRequestEntity rptRequestEntity) throws ConversionException {

        RPTRequest response;
        try {
            byte[] payloadUnzipped = ZipUtil.unzip(ZipUtil.base64Decode(rptRequestEntity.getPayload()));
            Element envelopeElement = jaxbElementUtil.convertToEnvelopeElement(payloadUnzipped);
            Envelope envelope = jaxbElementUtil.convertToBean(envelopeElement, Envelope.class);
            Primitive primitive = Primitive.fromString(rptRequestEntity.getPrimitive());
            if (primitive == null) {
                throw new ConversionException(String.format("Unable to unmarshall RPT header or body. The string object refers to a primitive that is not handled by this service. Use one of the following: [%s]", Arrays.asList(Primitive.values())));
            }

            switch (primitive) {
                case NODO_INVIA_RPT -> response = RPTRequest.builder()
                        .header(jaxbElementUtil.getSoapHeader(envelope, IntestazionePPT.class))
                        .body(jaxbElementUtil.getSoapBody(envelope, NodoInviaRPT.class))
                        .build();
                case NODO_INVIA_CARRELLO_RPT -> response = RPTRequest.builder()
                        .header(jaxbElementUtil.getSoapHeader(envelope, IntestazioneCarrelloPPT.class))
                        .body(jaxbElementUtil.getSoapBody(envelope, NodoInviaCarrelloRPT.class))
                        .build();
                default ->
                        throw new ConversionException(String.format("Unable to unmarshall RPT header or body. No valid parsing process was defined for the primitive [%s].", primitive));
            }
        } catch (IOException e) {
            throw new ConversionException("Unable to unmarshall Envelope content. ", e);
        }

        return response;
    }

    @SuppressWarnings({"rawtypes"})
    private CtRichiestaPagamentoTelematico extractRPT(RPTContent rptContent) throws ConversionException {
        // extracting byte array containing Base64 of RPT
        byte[] rptBytes;
        Object wrapper = rptContent.getWrappedRPT();
        if (wrapper instanceof NodoInviaRPT nodoInviaRPT) {
            rptBytes = nodoInviaRPT.getRpt();
        } else if (wrapper instanceof TipoElementoListaRPT tipoElementoListaRPT) {
            rptBytes = tipoElementoListaRPT.getRpt();
        } else {
            throw new ConversionException("Unable to unmarshall RPT content for CtRichiestaPagamentoTelematico. Invalid class for RPT wrapper.");
        }
        // converting Base64 to XML string and unmarshalling content
        Element rptElement = jaxbElementUtil.convertToRPTElement(rptBytes);
        return jaxbElementUtil.convertToBean(rptElement, CtRichiestaPagamentoTelematico.class);
    }


    @SuppressWarnings({"rawtypes"})
    private PaymentPosition mapRPTToDebtPosition(RPTRequest rptRequest, RPTContent rptContent) throws ConversionException {

        // call IUV Generator API for generate NAV
        String creditorInstitutionCode = rptContent.getRpt().getDominio().getIdentificativoDominio();
        String navCode = this.navGeneratorService.getNAVCodeFromIUVGenerator(creditorInstitutionCode);

        // TODO mapping
        return null;
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
