package it.gov.pagopa.wispconverter.service;

import gov.telematici.pagamenti.ws.nodoperpa.NodoInviaCarrelloRPT;
import gov.telematici.pagamenti.ws.nodoperpa.NodoInviaRPT;
import gov.telematici.pagamenti.ws.nodoperpa.TipoElementoListaRPT;
import gov.telematici.pagamenti.ws.nodoperpa.ppthead.IntestazioneCarrelloPPT;
import gov.telematici.pagamenti.ws.nodoperpa.ppthead.IntestazionePPT;
import it.gov.digitpa.schemas._2011.pagamenti.CtRichiestaPagamentoTelematico;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.model.enumz.InternalStepStatus;
import it.gov.pagopa.wispconverter.service.mapper.RPTMapper;
import it.gov.pagopa.wispconverter.service.model.paymentrequest.PaymentRequestDTO;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.service.model.session.CommonFieldsDTO;
import it.gov.pagopa.wispconverter.service.model.session.RPTContentDTO;
import it.gov.pagopa.wispconverter.service.model.session.SessionDataDTO;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.JaxbElementUtil;
import it.gov.pagopa.wispconverter.util.ReUtil;
import it.gov.pagopa.wispconverter.util.ZipUtil;
import jakarta.xml.soap.SOAPMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RPTExtractorService {

    private final ReService reService;

    private final JaxbElementUtil jaxbElementUtil;

    private final RPTMapper mapper;

    @Value("${wisp-converter.re-tracing.internal.rpt-extraction.enabled}")
    private Boolean isTracingOnREEnabled;

    public SessionDataDTO extractSessionData(String primitive, String payload) {

        // extracting body from SOAP Envelope body
        SOAPMessage soapMessage;
        try {
            byte[] payloadUnzipped = ZipUtil.unzip(ZipUtil.base64Decode(payload));
            soapMessage = this.jaxbElementUtil.getMessage(payloadUnzipped);
        } catch (IOException e) {
            throw new AppException(AppErrorCodeMessageEnum.PARSING_INVALID_ZIPPED_PAYLOAD);
        }

        // extracting session data
        SessionDataDTO sessionData;
        switch (primitive) {
            case Constants.NODO_INVIA_RPT -> sessionData = extractSessionDataFromNodoInviaRPT(soapMessage);
            case Constants.NODO_INVIA_CARRELLO_RPT ->
                    sessionData = extractSessionDataFromNodoInviaCarrelloRPT(soapMessage);
            default -> throw new AppException(AppErrorCodeMessageEnum.PARSING_RPT_PRIMITIVE_NOT_VALID);
        }

        // generate and save RE event internal for change status
        setSessionDataInfoInMDC(sessionData, primitive);
        generateRE(sessionData);

        return sessionData;
    }

    private SessionDataDTO extractSessionDataFromNodoInviaRPT(SOAPMessage soapMessage) {

        // extracting header and body from SOAP envelope
        IntestazionePPT soapHeader = this.jaxbElementUtil.getHeader(soapMessage, IntestazionePPT.class);
        NodoInviaRPT soapBody = this.jaxbElementUtil.getBody(soapMessage, NodoInviaRPT.class);

        // initializing common fields
        String creditorInstitutionId = soapHeader.getIdentificativoDominio();
        PaymentRequestDTO rpt = extractRPT(soapBody.getRpt());
        boolean containsDigitalStamp = rpt.getTransferData().getTransfer().stream().anyMatch(transfer -> transfer.getDigitalStamp() != null);

        // finally, generate session data
        return SessionDataDTO.builder()
                .commonFields(CommonFieldsDTO.builder()
                        .sessionId(MDC.get(Constants.MDC_SESSION_ID))
                        .creditorInstitutionId(creditorInstitutionId)
                        .pspId(soapBody.getIdentificativoPSP())
                        .creditorInstitutionBrokerId(soapHeader.getIdentificativoIntermediarioPA())
                        .stationId(soapHeader.getIdentificativoStazioneIntermediarioPA())
                        .channelId(soapBody.getIdentificativoCanale())
                        .payerType(rpt.getPayer().getSubjectUniqueIdentifier().getType())
                        .payerFiscalCode(rpt.getPayer().getSubjectUniqueIdentifier().getCode())
                        .payerFullName(rpt.getPayer().getName())
                        .payerType(rpt.getPayer().getSubjectUniqueIdentifier().getType())
                        .payerFiscalCode(rpt.getPayer().getSubjectUniqueIdentifier().getCode())
                        .payerFullName(rpt.getPayer().getName())
                        .payerAddressStreetName(rpt.getPayer().getAddress())
                        .payerAddressStreetNumber(rpt.getPayer().getStreetNumber())
                        .payerAddressPostalCode(rpt.getPayer().getPostalCode())
                        .payerAddressCity(rpt.getPayer().getCity())
                        .payerAddressProvince(rpt.getPayer().getProvince())
                        .payerAddressNation(rpt.getPayer().getNation())
                        .payerEmail(rpt.getPayer().getEmail())
                        .isMultibeneficiary(false)
                        .containsDigitalStamp(containsDigitalStamp)
                        .build())
                .paymentNotices(new HashMap<>())
                .rpts(Collections.singletonMap(rpt.getTransferData().getIuv(), RPTContentDTO.builder()
                        .iupd(soapHeader.getIdentificativoIntermediarioPA() + soapHeader.getIdentificativoUnivocoVersamento())
                        .iuv(rpt.getTransferData().getIuv())
                        .rpt(rpt)
                        .ccp(rpt.getTransferData().getCcp())
                        .index(1)
                        .containsDigitalStamp(containsDigitalStamp)
                        .build()))
                .build();
    }

    private SessionDataDTO extractSessionDataFromNodoInviaCarrelloRPT(SOAPMessage soapMessage) {

        // extracting header and body from SOAP envelope
        IntestazioneCarrelloPPT soapHeader = this.jaxbElementUtil.getHeader(soapMessage, IntestazioneCarrelloPPT.class);
        NodoInviaCarrelloRPT soapBody = this.jaxbElementUtil.getBody(soapMessage, NodoInviaCarrelloRPT.class);

        // initializing common fields
        boolean isMultibeneficiary = soapBody.isMultiBeneficiario() != null && soapBody.isMultiBeneficiario();
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

        // extracting
        List<RPTContentDTO> rptContents = new LinkedList<>();
        int rptIndex = 1;
        for (TipoElementoListaRPT elementoListaRPT : soapBody.getListaRPT().getElementoListaRPT()) {

            // generating RPT
            PaymentRequestDTO rpt = extractRPT(elementoListaRPT.getRpt());

            /*
              Validating common fields.
              These fields will be equals for each RPT, so it could be set from 0-index element.
              But this strategy is used to check the uniqueness of these fields for each RPT and if this is
              not true, an exception is thrown.
             */
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

            // generating content for RPT
            rptContents.add(RPTContentDTO.builder()
                    .iupd(soapHeader.getIdentificativoIntermediarioPA() + soapHeader.getIdentificativoCarrello())
                    .iuv(rpt.getTransferData().getIuv())
                    .ccp(rpt.getTransferData().getCcp())
                    .containsDigitalStamp(rpt.getTransferData().getTransfer().stream().anyMatch(transfer -> transfer.getDigitalStamp() != null))
                    .rpt(rpt)
                    .index(rptIndex)
                    .build());

            // increment RPT index
            rptIndex++;
        }

        // finally, generate session data
        return SessionDataDTO.builder()
                .commonFields(CommonFieldsDTO.builder()
                        .sessionId(MDC.get(Constants.MDC_SESSION_ID))
                        .cartId(soapHeader.getIdentificativoCarrello())
                        .creditorInstitutionId(creditorInstitutionId)
                        .pspId(soapBody.getIdentificativoPSP())
                        .creditorInstitutionBrokerId(soapHeader.getIdentificativoIntermediarioPA())
                        .stationId(soapHeader.getIdentificativoStazioneIntermediarioPA())
                        .channelId(soapBody.getIdentificativoCanale())
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
                        .containsDigitalStamp(rptContents.stream().anyMatch(RPTContentDTO::getContainsDigitalStamp))
                        .build())
                .paymentNotices(new HashMap<>())
                .rpts(rptContents.stream().collect(Collectors.toMap(RPTContentDTO::getIuv, Function.identity())))
                .build();
    }

    private <T> T checkUniqueness(T existingValue, T newValue, AppErrorCodeMessageEnum error) {

        if (existingValue != null && !existingValue.equals(newValue)) {
            throw new AppException(error);
        }
        return newValue;
    }

    private PaymentRequestDTO extractRPT(byte[] rptBytes) {

        CtRichiestaPagamentoTelematico rptElement = this.jaxbElementUtil.convertToBean(rptBytes, CtRichiestaPagamentoTelematico.class);
        return mapper.toPaymentRequestDTO(rptElement);
    }

    private void generateRE(SessionDataDTO sessionData) {

        // creating event to be persisted for RE
        if (Boolean.TRUE.equals(isTracingOnREEnabled)) {
            for (RPTContentDTO rpt : sessionData.getAllRPTs()) {
                ReEventDto reEventFromRPT = ReUtil.getREBuilder()
                        .status(InternalStepStatus.EXTRACTED_DATA_FROM_RPT)
                        .iuv(rpt.getIuv())
                        .ccp(rpt.getCcp())
                        .build();
                reService.addRe(reEventFromRPT);
            }
        }
    }

    private void setSessionDataInfoInMDC(SessionDataDTO sessionData, String primitive) {

        CommonFieldsDTO commonFields = sessionData.getCommonFields();
        MDC.put(Constants.MDC_PRIMITIVE, primitive);
        MDC.put(Constants.MDC_CART_ID, commonFields.getCartId());
        MDC.put(Constants.MDC_DOMAIN_ID, commonFields.getCreditorInstitutionId());
        MDC.put(Constants.MDC_STATION_ID, commonFields.getStationId());
        MDC.put(Constants.MDC_CHANNEL_ID, commonFields.getChannelId());
        MDC.put(Constants.MDC_PSP_ID, commonFields.getPspId());

        // if the primitive is nodoInviaCarrelloRPT, it means that a cart was extracted, so set cartId in MDC. Otherwise, set IUV and CCP in MDC
        if (Constants.NODO_INVIA_CARRELLO_RPT.equals(primitive)) {
            MDC.put(Constants.MDC_CART_ID, commonFields.getCartId());
        } else {
            RPTContentDTO singleRpt = sessionData.getFirstRPT();
            MDC.put(Constants.MDC_IUV, singleRpt.getIuv());
            MDC.put(Constants.MDC_CCP, singleRpt.getCcp());
        }
    }
}
