package it.gov.pagopa.wispconverter.service;

import feign.FeignException;
import gov.telematici.pagamenti.ws.NodoInviaCarrelloRPT;
import gov.telematici.pagamenti.ws.NodoInviaRPT;
import gov.telematici.pagamenti.ws.TipoElementoListaRPT;
import gov.telematici.pagamenti.ws.ppthead.IntestazioneCarrelloPPT;
import gov.telematici.pagamenti.ws.ppthead.IntestazionePPT;
import it.gov.digitpa.schemas._2011.pagamenti.CtRichiestaPagamentoTelematico;
import it.gov.pagopa.wispconverter.client.iuvgenerator.IUVGeneratorClient;
import it.gov.pagopa.wispconverter.client.iuvgenerator.model.IUVGeneratorRequest;
import it.gov.pagopa.wispconverter.client.iuvgenerator.model.IUVGeneratorResponse;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.service.mapper.RPTMapper;
import it.gov.pagopa.wispconverter.service.model.CommonRPTFieldsDTO;
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
import java.util.LinkedList;
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

    public CommonRPTFieldsDTO extractRPTContentDTOs(String primitive, String payload) {

        Envelope envelope;
        try {
            byte[] payloadUnzipped = ZipUtil.unzip(ZipUtil.base64Decode(payload));
            Element envelopeElement = this.jaxbElementUtil.convertToEnvelopeElement(payloadUnzipped);
            envelope = this.jaxbElementUtil.convertToBean(envelopeElement, Envelope.class);
        } catch (IOException e) {
            throw new AppException(AppErrorCodeMessageEnum.PARSING_INVALID_ZIPPED_PAYLOAD);
        }

        CommonRPTFieldsDTO commonRPTFieldsDTO;
        switch (primitive) {
            case "nodoInviaRPT" -> commonRPTFieldsDTO = extractRPTContentDTOsFromNodoInviaRPT(envelope);
            case "nodoInviaCarrelloRPT" -> commonRPTFieldsDTO = extractRPTContentDTOsFromNodoInviaCarrelloRPT(envelope);
            default -> throw new AppException(AppErrorCodeMessageEnum.PARSING_PRIMITIVE_NOT_VALID);
        }
        return commonRPTFieldsDTO;
    }

    private CommonRPTFieldsDTO extractRPTContentDTOsFromNodoInviaRPT(Envelope envelope) {
        IntestazionePPT soapHeader = this.jaxbElementUtil.getSoapHeader(envelope, IntestazionePPT.class);
        NodoInviaRPT soapBody = this.jaxbElementUtil.getSoapBody(envelope, NodoInviaRPT.class);

        String creditorInstitutionId = soapHeader.getIdentificativoDominio();
        PaymentRequestDTO rpt = extractRPT(soapBody.getRpt());
        boolean containsDigitalStamp = rpt.getTransferData().getTransfer().stream().anyMatch(transfer -> transfer.getDigitalStamp() != null);

        return CommonRPTFieldsDTO.builder()
                .iupd(soapHeader.getIdentificativoIntermediarioPA() + soapHeader.getIdentificativoUnivocoVersamento())
                .iuv(rpt.getTransferData().getIuv())
                .nav(getNAVCodeFromIUVGenerator(creditorInstitutionId))
                .creditorInstitutionId(creditorInstitutionId)
                .creditorInstitutionBrokerId(soapHeader.getIdentificativoIntermediarioPA())
                .stationId(soapHeader.getIdentificativoStazioneIntermediarioPA())
                .payerType(rpt.getPayer().getSubjectUniqueIdentifier().getType())
                .payerFiscalCode(rpt.getPayer().getSubjectUniqueIdentifier().getCode())
                .payerFullName(rpt.getPayer().getName())
                .isMultibeneficiary(false)
                .containsDigitalStamp(containsDigitalStamp)
                .rpts(Collections.singletonList(RPTContentDTO.builder()
                        .rpt(rpt)
                        .containsDigitalStamp(containsDigitalStamp)
                        .build()))
                .build();
    }

    private CommonRPTFieldsDTO extractRPTContentDTOsFromNodoInviaCarrelloRPT(Envelope envelope) {
        IntestazioneCarrelloPPT soapHeader = this.jaxbElementUtil.getSoapHeader(envelope, IntestazioneCarrelloPPT.class);
        NodoInviaCarrelloRPT soapBody = this.jaxbElementUtil.getSoapBody(envelope, NodoInviaCarrelloRPT.class);

        // initializing common fields
        boolean isMultibeneficiary = soapBody.isMultiBeneficiario();
        String creditorInstitutionId = null;
        String payerType = null;
        String payerFiscalCode = null;
        String fullName = null;
        String iuv = null;
        String streetName = null;
        String streetNumber = null;
        String postalCode = null;
        String city = null;
        String province = null;
        String nation = null;
        String email = null;

        List<RPTContentDTO> rptContentDTOs = new LinkedList<>();
        for (TipoElementoListaRPT elementoListaRPT : soapBody.getListaRPT().getElementoListaRPT()) {

            // generating RPT
            PaymentRequestDTO rpt = extractRPT(elementoListaRPT.getRpt());
            // validating common fields
            creditorInstitutionId = checkUniqueness(creditorInstitutionId, isMultibeneficiary ?
                    soapHeader.getIdentificativoCarrello().substring(0, 11) :
                    rpt.getDomain().getDomainId());
            payerType = checkUniqueness(payerType, rpt.getPayer().getSubjectUniqueIdentifier().getType());
            payerFiscalCode = checkUniqueness(payerFiscalCode, rpt.getPayer().getSubjectUniqueIdentifier().getCode());
            fullName = checkUniqueness(fullName, rpt.getPayer().getName());
            iuv = checkUniqueness(iuv, rpt.getTransferData().getIuv());
            streetName = checkUniqueness(streetName, rpt.getPayer().getAddress());
            streetNumber = checkUniqueness(streetNumber, rpt.getPayer().getStreetNumber());
            postalCode = checkUniqueness(postalCode, rpt.getPayer().getPostalCode());
            city = checkUniqueness(city, rpt.getPayer().getCity());
            province = checkUniqueness(province, rpt.getPayer().getProvince());
            nation = checkUniqueness(nation, rpt.getPayer().getNation());
            email = checkUniqueness(email, rpt.getPayer().getEmail());
            // generating RPT content DTO
            rptContentDTOs.add(RPTContentDTO.builder()
                    .containsDigitalStamp(rpt.getTransferData().getTransfer().stream().anyMatch(transfer -> transfer.getDigitalStamp() != null))
                    .rpt(rpt)
                    .build());
        }

        return CommonRPTFieldsDTO.builder()
                .cartId(soapHeader.getIdentificativoCarrello())
                .iupd(soapHeader.getIdentificativoIntermediarioPA() + soapHeader.getIdentificativoCarrello())
                .iuv(iuv)
                .nav(getNAVCodeFromIUVGenerator(creditorInstitutionId))
                .creditorInstitutionId(creditorInstitutionId)
                .creditorInstitutionBrokerId(soapHeader.getIdentificativoIntermediarioPA())
                .payerType(payerType)
                .payerFiscalCode(payerFiscalCode)
                .payerFullName(fullName)
                .payerAddressStreetName(streetName)
                .payerAddressStreetNumber(streetNumber)
                .payerAddressPostalCode(postalCode)
                .payerAddressCity(city)
                .payerAddressProvince(province)
                .payerAddressNation(nation)
                .payerEmail(email)
                .isMultibeneficiary(isMultibeneficiary)
                .containsDigitalStamp(rptContentDTOs.stream().anyMatch(RPTContentDTO::getContainsDigitalStamp))
                .rpts(rptContentDTOs)
                .build();
    }

    private <T> T checkUniqueness(T existingValue, T newValue) {
        if (existingValue != null && !existingValue.equals(newValue)) {
            throw new AppException(AppErrorCodeMessageEnum.GENERIC_ERROR); // TODO to be changed on mapping disambiguation
        }
        return newValue;
    }

    private PaymentRequestDTO extractRPT(byte[] rptBytes) {
        Element rptElement = this.jaxbElementUtil.convertToRPTElement(rptBytes);
        return mapper.toPaymentRequestDTO(this.jaxbElementUtil.convertToBean(rptElement, CtRichiestaPagamentoTelematico.class));
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
            navCode = response.getIuv();
        } catch (FeignException e) {
            throw new AppException(AppErrorCodeMessageEnum.CLIENT_IUVGENERATOR_INVALID_RESPONSE, e.status(), e.getMessage());
        }
        return navCode;
    }
}
