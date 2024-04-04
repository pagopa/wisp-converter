package it.gov.pagopa.wispconverter.service;

import gov.telematici.pagamenti.ws.NodoInviaCarrelloRPT;
import gov.telematici.pagamenti.ws.NodoInviaRPT;
import gov.telematici.pagamenti.ws.ppthead.IntestazioneCarrelloPPT;
import gov.telematici.pagamenti.ws.ppthead.IntestazionePPT;
import it.gov.digitpa.schemas._2011.pagamenti.CtRichiestaPagamentoTelematico;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.service.model.ConversionResultDTO;
import it.gov.pagopa.wispconverter.service.model.RPTContentDTO;
import it.gov.pagopa.wispconverter.util.FileReader;
import it.gov.pagopa.wispconverter.util.JaxbElementUtil;
import it.gov.pagopa.wispconverter.util.ZipUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.xmlsoap.schemas.soap.envelope.Envelope;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConverterService {

    private final NAVGeneratorService navGeneratorService;

    private final DebtPositionService debtPositionService;

    private final CacheService cacheService;

    private final CheckoutService checkoutService;

    private final RPTRequestRepository rptRequestRepository;

    private final JaxbElementUtil jaxbElementUtil;

    public String convert(String sessionId) {

        // get request entity from CosmosDB
        RPTRequestEntity rptRequestEntity = getRPTRequestEntity(sessionId);

        // unmarshalling header and body from request entity
        List<RPTContentDTO> rptContentDTOs = getRPTContentDTO(rptRequestEntity.getPrimitive(), rptRequestEntity.getPayload());

        // extracting creditor institution code from header and call GPD bulk creation API
        this.debtPositionService.executeBulkCreation(rptContentDTOs);

        // call APIM policy for save key for decoupler and save in Redis cache the mapping of the request identifier needed for RT generation in next steps
        this.cacheService.storeRequestMappingInCache(rptContentDTOs, sessionId);

        // execute communication with Checkout service and set the redirection URI as response
        String redirectURI = this.checkoutService.executeCall();
        return redirectURI;
    }



    private RPTRequestEntity getRPTRequestEntity(String sessionId) {
        Optional<RPTRequestEntity> optRPTReqEntity = this.rptRequestRepository.findById(sessionId);
        return optRPTReqEntity.orElseThrow(() -> new AppException(AppErrorCodeMessageEnum.RPT_NOT_FOUND, sessionId));

        // TODO RE
    }

    private List<RPTContentDTO> getRPTContentDTO(String primitive, String payload) {

        byte[] payloadUnzipped = new byte[0];
        try {
            payloadUnzipped = ZipUtil.unzip(ZipUtil.base64Decode(payload));
        } catch (IOException e) {
            throw new AppException(e, AppErrorCodeMessageEnum.UNZIP, e.getMessage());
        }
        Element envelopeElement = jaxbElementUtil.convertToEnvelopeElement(payloadUnzipped);
        Envelope envelope = jaxbElementUtil.convertToBean(envelopeElement, Envelope.class);

        switch (primitive) {
            case "nodoInviaRPT" -> {
                IntestazionePPT soapHeader = jaxbElementUtil.getSoapHeader(envelope, IntestazionePPT.class);
                NodoInviaRPT soapBody = jaxbElementUtil.getSoapBody(envelope, NodoInviaRPT.class);
                String idDominio = soapHeader.getIdentificativoDominio();
                return Collections.singletonList(RPTContentDTO.builder()
                        .idDominio(idDominio)
                        .noticeNumber(this.navGeneratorService.getNAVCodeFromIUVGenerator(idDominio))
                        .rpt(getRPT(soapBody.getRpt()))
                        .build());
            }
            case "nodoInviaCarrelloRPT" -> {
                IntestazioneCarrelloPPT soapHeader = jaxbElementUtil.getSoapHeader(envelope, IntestazioneCarrelloPPT.class);
                NodoInviaCarrelloRPT soapBody = jaxbElementUtil.getSoapBody(envelope, NodoInviaCarrelloRPT.class);

                return soapBody.getListaRPT().getElementoListaRPT().stream().map(a -> {
                    boolean isMultibeneficiario = soapBody.isMultiBeneficiario();
                    CtRichiestaPagamentoTelematico rpt = getRPT(a.getRpt());
                    String idDominio = isMultibeneficiario ?
                            soapHeader.getIdentificativoCarrello().substring(0, 11) :
                            rpt.getDominio().getIdentificativoDominio();

                    return RPTContentDTO.builder()
                            .idDominio(idDominio)
                            .noticeNumber(this.navGeneratorService.getNAVCodeFromIUVGenerator(idDominio))
                            .multibeneficiario(isMultibeneficiario)
                            .rpt(rpt)
                            .build();
                }).toList();
            }
            default -> throw new AppException(AppErrorCodeMessageEnum.PRIMITIVE_NOT_VALID, primitive);
        }
    }

    private CtRichiestaPagamentoTelematico getRPT(byte[] rptBytes) {
        Element rptElement = jaxbElementUtil.convertToRPTElement(rptBytes);
        return jaxbElementUtil.convertToBean(rptElement, CtRichiestaPagamentoTelematico.class);
    }


}
