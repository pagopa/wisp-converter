package it.gov.pagopa.wispconverter.service;

import com.azure.cosmos.CosmosException;
import com.azure.spring.data.cosmos.exception.CosmosAccessException;
import gov.telematici.pagamenti.ws.NodoInviaCarrelloRPT;
import gov.telematici.pagamenti.ws.NodoInviaRPT;
import gov.telematici.pagamenti.ws.ppthead.IntestazioneCarrelloPPT;
import gov.telematici.pagamenti.ws.ppthead.IntestazionePPT;
import it.gov.digitpa.schemas._2011.pagamenti.CtRichiestaPagamentoTelematico;
import it.gov.pagopa.wispconverter.entity.Primitive;
import it.gov.pagopa.wispconverter.entity.RPTRequestEntity;
import it.gov.pagopa.wispconverter.exception.conversion.ConversionException;
import it.gov.pagopa.wispconverter.model.converter.ConversionResult;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.util.FileReader;
import it.gov.pagopa.wispconverter.util.JaxbElementUtil;
import it.gov.pagopa.wispconverter.util.ZipUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.xmlsoap.schemas.soap.envelope.Envelope;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConverterService {

    private final NAVGeneratorService navGeneratorService;

    private final RPTRequestRepository rptRequestRepository;

    private final CacheRepository cacheRepository;

    private final FileReader fileReader;

    private final JaxbElementUtil jaxbElementUtil;


    public ConversionResult convert(String sessionId) {

        ConversionResult conversionResult = null;
        try {
            // get request from CosmosDB
            RPTRequestEntity rptRequestEntity = getRPTRequestEntity(sessionId);
            byte[] payloadUnzipped = ZipUtil.unzip(ZipUtil.base64Decode(rptRequestEntity.getPayload()));

            Element envelopeElement = jaxbElementUtil.convertToEnvelopeElement(payloadUnzipped);
            Envelope envelope = jaxbElementUtil.convertToBean(envelopeElement, Envelope.class);

            Primitive primitive = mapPrimitive(rptRequestEntity.getPrimitive());
            switch (primitive) {
                case NODO_INVIA_RPT -> {
                    IntestazionePPT intestazionePPT = jaxbElementUtil.getSoapHeader(envelope, IntestazionePPT.class);
                    NodoInviaRPT nodoInviaRPT = jaxbElementUtil.getSoapBody(envelope, NodoInviaRPT.class);
                    byte[] rptBytes = nodoInviaRPT.getRpt();
                    Element rptElement = jaxbElementUtil.convertToRPTElement(rptBytes);
                    CtRichiestaPagamentoTelematico ctRichiestaPagamentoTelematico = jaxbElementUtil.convertToBean(rptElement, CtRichiestaPagamentoTelematico.class);
                    log.info(nodoInviaRPT.getIdentificativoCanale());
                }
                case NODO_INVIA_CARRELLO_RPT -> {
                    IntestazioneCarrelloPPT intestazioneCarrelloPPT = jaxbElementUtil.getSoapHeader(envelope, IntestazioneCarrelloPPT.class);
                    NodoInviaCarrelloRPT nodoInviaCarrelloRPT = jaxbElementUtil.getSoapBody(envelope, NodoInviaCarrelloRPT.class);
                    nodoInviaCarrelloRPT.getListaRPT().getElementoListaRPT().forEach(tipoElementoListaRPT -> {
                        byte[] rptBytes = tipoElementoListaRPT.getRpt();
                        Element rptElement = jaxbElementUtil.convertToRPTElement(rptBytes);
                        CtRichiestaPagamentoTelematico ctRichiestaPagamentoTelematico = jaxbElementUtil.convertToBean(rptElement, CtRichiestaPagamentoTelematico.class);
                        log.info(tipoElementoListaRPT.getIdentificativoUnivocoVersamento());
                    });
                }
                default -> throw new ConversionException(String.format("Unable to unmarshall RPT header or body. No valid parsing process was defined for the primitive [%s].", primitive));
            }



            // parse header and body from request entity
//            RPTRequest rptRequest = this.unmarshallerService.unmarshall(rptRequestEntity);

            // for each RPT in list
            // call IUV Generator API for generate NAV
//            List<byte[]> rawRPTs = CommonUtility.getAllRawRPTs(rptRequest);
//            String creditorInstitutionCode = CommonUtility.getCreditorInstitutionCode(rptRequest);
//            String navCode = this.navGeneratorService.getNAVCodeFromIUVGenerator(creditorInstitutionCode);

            // execute mapping of COSMOSDB data for GPD insert
//            this.unmarshallerService.unmarshall(rptRequestEntity);

            // call GPD bulk creation API

            // call APIM policy for save key for decoupler

            // save key in REDIS for 'primitiva di innesco'

            // execute mapping for Checkout carts invocation

            // call Checkout carts API

            // receive Checkout response and returns redirection URI
        } catch (ConversionException e) {
            log.error("Error while executing RPT conversion. ", e);
            conversionResult = getHTMLErrorPage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        return conversionResult;
    }

    private Primitive mapPrimitive(String primitiva) throws ConversionException {
        Primitive primitive = Primitive.fromString(primitiva);
        if (primitive == null) {
            throw new ConversionException(String.format("Unable to unmarshall RPT header or body. The string object refers to a primitive that is not handled by this service. Use one of the following: [%s]", Arrays.asList(Primitive.values())));
        }
        return primitive;
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
