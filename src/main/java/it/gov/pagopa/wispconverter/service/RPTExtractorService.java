package it.gov.pagopa.wispconverter.service;

import gov.telematici.pagamenti.ws.NodoInviaCarrelloRPT;
import gov.telematici.pagamenti.ws.NodoInviaRPT;
import gov.telematici.pagamenti.ws.TipoElementoListaRPT;
import gov.telematici.pagamenti.ws.ppthead.IntestazioneCarrelloPPT;
import gov.telematici.pagamenti.ws.ppthead.IntestazionePPT;
import it.gov.digitpa.schemas._2011.pagamenti.CtRichiestaPagamentoTelematico;
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
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.xmlsoap.schemas.soap.envelope.Envelope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RPTExtractorService {

    private final JaxbElementUtil jaxbElementUtil;

    private final RPTMapper mapper;

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
                .creditorInstitutionId(creditorInstitutionId)
                .creditorInstitutionBrokerId(soapHeader.getIdentificativoIntermediarioPA())
                .stationId(soapHeader.getIdentificativoStazioneIntermediarioPA())
                .payerType(rpt.getPayer().getSubjectUniqueIdentifier().getType())
                .payerFiscalCode(rpt.getPayer().getSubjectUniqueIdentifier().getCode())
                .payerFullName(rpt.getPayer().getName())
                .isMultibeneficiary(false)
                .containsDigitalStamp(containsDigitalStamp)
                .paymentNotices(new ArrayList<>())
                .rpts(Collections.singletonList(RPTContentDTO.builder()
                        .iupd(soapHeader.getIdentificativoIntermediarioPA() + soapHeader.getIdentificativoUnivocoVersamento())
                        .iuv(rpt.getTransferData().getIuv())
                        .rpt(rpt)
                        .containsDigitalStamp(containsDigitalStamp)
                        .build()))
                .build();
    }

    private CommonRPTFieldsDTO extractRPTContentDTOsFromNodoInviaCarrelloRPT(Envelope envelope) {
        IntestazioneCarrelloPPT soapHeader = this.jaxbElementUtil.getSoapHeader(envelope, IntestazioneCarrelloPPT.class);
        NodoInviaCarrelloRPT soapBody = this.jaxbElementUtil.getSoapBody(envelope, NodoInviaCarrelloRPT.class);

        // initializing common fields
        boolean isMultibeneficiary = soapBody.isMultiBeneficiario() !=null && soapBody.isMultiBeneficiario();
        String creditorInstitutionId = null;
        String payerType = null;
        String payerFiscalCode = null;
        String fullName = null;
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
            creditorInstitutionId = isMultibeneficiary ?
                    soapHeader.getIdentificativoCarrello().substring(0, 11) :
                    checkUniqueness(creditorInstitutionId, rpt.getDomain().getDomainId(), AppErrorCodeMessageEnum.VALIDATION_INVALID_CREDITOR_INSTITUTION);
            payerType = checkUniqueness(payerType, rpt.getPayer().getSubjectUniqueIdentifier().getType(), AppErrorCodeMessageEnum.VALIDATION_INVALID_DEBTOR);
            payerFiscalCode = checkUniqueness(payerFiscalCode, rpt.getPayer().getSubjectUniqueIdentifier().getCode(), AppErrorCodeMessageEnum.VALIDATION_INVALID_DEBTOR);
            fullName = checkUniqueness(fullName, rpt.getPayer().getName(), AppErrorCodeMessageEnum.VALIDATION_INVALID_DEBTOR);
            streetName = checkUniqueness(streetName, rpt.getPayer().getAddress(), AppErrorCodeMessageEnum.VALIDATION_INVALID_DEBTOR);
            streetNumber = checkUniqueness(streetNumber, rpt.getPayer().getStreetNumber(), AppErrorCodeMessageEnum.VALIDATION_INVALID_DEBTOR);
            postalCode = checkUniqueness(postalCode, rpt.getPayer().getPostalCode(), AppErrorCodeMessageEnum.VALIDATION_INVALID_DEBTOR);
            city = checkUniqueness(city, rpt.getPayer().getCity(), AppErrorCodeMessageEnum.VALIDATION_INVALID_DEBTOR);
            province = checkUniqueness(province, rpt.getPayer().getProvince(), AppErrorCodeMessageEnum.VALIDATION_INVALID_DEBTOR);
            nation = checkUniqueness(nation, rpt.getPayer().getNation(), AppErrorCodeMessageEnum.VALIDATION_INVALID_DEBTOR);
            email = checkUniqueness(email, rpt.getPayer().getEmail(), AppErrorCodeMessageEnum.VALIDATION_INVALID_DEBTOR);
            // generating RPT content DTO
            rptContentDTOs.add(RPTContentDTO.builder()
                    .iupd(soapHeader.getIdentificativoIntermediarioPA() + soapHeader.getIdentificativoCarrello())
                    .iuv(rpt.getTransferData().getIuv())
                    .containsDigitalStamp(rpt.getTransferData().getTransfer().stream().anyMatch(transfer -> transfer.getDigitalStamp() != null))
                    .rpt(rpt)
                    .build());
        }

        return CommonRPTFieldsDTO.builder()
                .cartId(soapHeader.getIdentificativoCarrello())
                .creditorInstitutionId(creditorInstitutionId)
                .creditorInstitutionBrokerId(soapHeader.getIdentificativoIntermediarioPA())
                .stationId(soapHeader.getIdentificativoStazioneIntermediarioPA())
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
                .paymentNotices(new ArrayList<>())
                .rpts(rptContentDTOs)
                .build();
    }

    private <T> T checkUniqueness(T existingValue, T newValue, AppErrorCodeMessageEnum error) {
        if (existingValue != null && !existingValue.equals(newValue)) {
            throw new AppException(error);
        }
        return newValue;
    }

    private PaymentRequestDTO extractRPT(byte[] rptBytes) {
        Element rptElement = this.jaxbElementUtil.convertToRPTElement(rptBytes);
        return mapper.toPaymentRequestDTO(this.jaxbElementUtil.convertToBean(rptElement, CtRichiestaPagamentoTelematico.class));
    }
}
