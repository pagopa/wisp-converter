package it.gov.pagopa.wispconverter.service;

import feign.FeignException;
import gov.telematici.pagamenti.ws.NodoInviaCarrelloRPT;
import gov.telematici.pagamenti.ws.NodoInviaRPT;
import gov.telematici.pagamenti.ws.ppthead.IntestazioneCarrelloPPT;
import gov.telematici.pagamenti.ws.ppthead.IntestazionePPT;
import it.gov.digitpa.schemas._2011.pagamenti.CtRichiestaPagamentoTelematico;
import it.gov.pagopa.wispconverter.client.iuvgenerator.IUVGeneratorClient;
import it.gov.pagopa.wispconverter.client.iuvgenerator.model.IUVGeneratorRequest;
import it.gov.pagopa.wispconverter.client.iuvgenerator.model.IUVGeneratorResponse;
import it.gov.pagopa.wispconverter.exception.AppError;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.service.mapper.RPTMapper;
import it.gov.pagopa.wispconverter.service.model.RPTContentDTO;
import it.gov.pagopa.wispconverter.service.model.paymentrequest.PaymentRequestDTO;
import it.gov.pagopa.wispconverter.util.JaxbElementUtil;
import it.gov.pagopa.wispconverter.util.ZipUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.xmlsoap.schemas.soap.envelope.Envelope;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RPTExtractorService {

    private final JaxbElementUtil jaxbElementUtil;

    private final IUVGeneratorClient iuvGeneratorClient;

    private final RPTMapper mapper;

    @Value("${wisp-converter.aux-digit}")
    private String auxDigit;

    @Value("${wisp-converter.segregation-code}")
    private String segregationCode;

    public List<RPTContentDTO> extractRPTContentDTOs(String primitive, String payload) throws IOException {

        byte[] payloadUnzipped = ZipUtil.unzip(ZipUtil.base64Decode(payload));
        Element envelopeElement = jaxbElementUtil.convertToEnvelopeElement(payloadUnzipped);
        Envelope envelope = jaxbElementUtil.convertToBean(envelopeElement, Envelope.class);

        List<RPTContentDTO> rptContentDTOs;
        switch (primitive) {
            case "nodoInviaRPT" -> rptContentDTOs = extractRPTContentDTOsFromNodoInviaRPT(envelope);
            case "nodoInviaCarrelloRPT" -> rptContentDTOs = extractRPTContentDTOsFromNodoInviaCarrelloRPT(envelope);
            default -> throw new AppException(AppError.UNKNOWN);
        }
        return rptContentDTOs;
    }

    private List<RPTContentDTO> extractRPTContentDTOsFromNodoInviaRPT(Envelope envelope) {
        IntestazionePPT soapHeader = jaxbElementUtil.getSoapHeader(envelope, IntestazionePPT.class);
        NodoInviaRPT soapBody = jaxbElementUtil.getSoapBody(envelope, NodoInviaRPT.class);
        String creditorInstitutionId = soapHeader.getIdentificativoDominio();
        return Collections.singletonList(RPTContentDTO.builder()
                .idDominio(creditorInstitutionId)
                .idIntermediarioPA(soapHeader.getIdentificativoIntermediarioPA())
                .nav(getNAVCodeFromIUVGenerator(creditorInstitutionId))
                .multibeneficiario(false)
                .rpt(extractRPT(soapBody.getRpt()))
                .build());
    }

    private List<RPTContentDTO> extractRPTContentDTOsFromNodoInviaCarrelloRPT(Envelope envelope) {
        IntestazioneCarrelloPPT soapHeader = jaxbElementUtil.getSoapHeader(envelope, IntestazioneCarrelloPPT.class);
        NodoInviaCarrelloRPT soapBody = jaxbElementUtil.getSoapBody(envelope, NodoInviaCarrelloRPT.class);

        return soapBody.getListaRPT().getElementoListaRPT().stream().map(elementoListaRPT -> {
            boolean isMultibeneficiary = soapBody.isMultiBeneficiario();
            PaymentRequestDTO rpt = extractRPT(elementoListaRPT.getRpt());
            String creditorInstitutionId = isMultibeneficiary ?
                    soapHeader.getIdentificativoCarrello().substring(0, 11) :
                    rpt.getDomain().getDomainId();

            return RPTContentDTO.builder()
                    .idDominio(creditorInstitutionId)
                    .idIntermediarioPA(soapHeader.getIdentificativoIntermediarioPA())
                    .nav(getNAVCodeFromIUVGenerator(creditorInstitutionId))
                    .multibeneficiario(isMultibeneficiary)
                    .rpt(rpt)
                    .build();
        }).toList();
    }

    private PaymentRequestDTO extractRPT(byte[] rptBytes) {
        Element rptElement = jaxbElementUtil.convertToRPTElement(rptBytes);
        return mapper.toPaymentRequestDTO(jaxbElementUtil.convertToBean(rptElement, CtRichiestaPagamentoTelematico.class));
    }

    private String getNAVCodeFromIUVGenerator(String creditorInstitutionCode) {
        // generating request body
        IUVGeneratorRequest request = IUVGeneratorRequest.builder()
                .auxDigit(this.auxDigit)
                .segregationCode(this.segregationCode)
                .build();
        // communicating with IUV Generator service in order to retrieve response
        String navCode;
        try {
            IUVGeneratorResponse response = this.iuvGeneratorClient.generate(creditorInstitutionCode, request);
            if (response == null) {
                throw new AppException(AppError.UNKNOWN);
            }
            navCode = response.getIuv();
        } catch (FeignException e) {
            throw new AppException(AppError.UNKNOWN);
        }
        return navCode;
    }
}
