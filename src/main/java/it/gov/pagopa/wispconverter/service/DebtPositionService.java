package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.client.gpd.api.DebtPositionsApiApi;
import it.gov.pagopa.wispconverter.client.gpd.model.*;
import it.gov.pagopa.wispconverter.client.iuvgenerator.api.IuvGeneratorApiApi;
import it.gov.pagopa.wispconverter.client.iuvgenerator.model.IuvGenerationModelDto;
import it.gov.pagopa.wispconverter.client.iuvgenerator.model.IuvGenerationModelResponseDto;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.service.mapper.DebtPositionMapper;
import it.gov.pagopa.wispconverter.service.model.*;
import it.gov.pagopa.wispconverter.service.model.paymentrequest.PaymentRequestDTO;
import it.gov.pagopa.wispconverter.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class DebtPositionService {

    private final it.gov.pagopa.wispconverter.client.gpd.invoker.ApiClient gpdClient;

    private final it.gov.pagopa.wispconverter.client.iuvgenerator.invoker.ApiClient iuvGeneratorClient;

    private final DebtPositionMapper mapper;

    private final Pattern taxonomyPattern = Pattern.compile("([^/]++/[^/]++)/?");

    @Value("${wisp-converter.poste-italiane.abi-code}")
    private String posteItalianeABICode;

    @Value("${wisp-converter.aux-digit}")
    private Long auxDigit;

    @Value("${wisp-converter.segregation-code}")
    private Long segregationCode;

    public void createDebtPositions(CommonRPTFieldsDTO rptContentDTOs) {

        try {
            // converting RPTs in single payment position
            MultiplePaymentPositionModelDto multiplePaymentPositions = extractPaymentPositions(rptContentDTOs);

            // communicating with GPD-core service in order to execute the operation
            DebtPositionsApiApi apiInstance = new DebtPositionsApiApi(gpdClient);
            apiInstance.createMultiplePositions1(rptContentDTOs.getCreditorInstitutionId(), multiplePaymentPositions, MDC.get(Constants.MDC_REQUEST_ID), true);

            //FIXME gestire errori di connessione
            //FIXME cosa succede se si spacca al secondo giro?
        } catch (RestClientException e) {
            throw new AppException(AppErrorCodeMessageEnum.CLIENT_GPD, e.getMessage());
        }
    }

    private MultiplePaymentPositionModelDto extractPaymentPositions(CommonRPTFieldsDTO commonRPTFieldsDTO) {

        List<PaymentPositionModelDto> paymentPositions;
        if (Boolean.TRUE.equals(commonRPTFieldsDTO.getIsMultibeneficiary())) {
            paymentPositions = extractPaymentPositionsForMultibeneficiary(commonRPTFieldsDTO);
        } else {
            paymentPositions = extractPaymentPositionsForNonMultibeneficiary(commonRPTFieldsDTO);
        }

        MultiplePaymentPositionModelDto multiplePaymentPosition = new MultiplePaymentPositionModelDto();
        multiplePaymentPosition.setPaymentPositions(paymentPositions);
        return multiplePaymentPosition;
    }

    private List<PaymentPositionModelDto> extractPaymentPositionsForMultibeneficiary(CommonRPTFieldsDTO commonRPTFieldsDTO) {

        if (commonRPTFieldsDTO.getRpts().size() < 2) {
            throw new AppException(AppErrorCodeMessageEnum.VALIDATION_INVALID_MULTIBENEFICIARY_CART);
        }
        RPTContentDTO firstRPTContentDTO = commonRPTFieldsDTO.getRpts().get(0);

        // mapping of transfers
        List<TransferModelDto> transfers = new ArrayList<>();
        for (RPTContentDTO rptContentDTO : commonRPTFieldsDTO.getRpts()) {

            PaymentRequestDTO paymentRequestDTO = rptContentDTO.getRpt();

            int transferIdCounter = 1;
            for (TransferDTO transferDTO : paymentRequestDTO.getTransferData().getTransfer()) {

                transfers.add(extractPaymentOptionTransfer(transferDTO, paymentRequestDTO.getDomain().getDomainId(), transferIdCounter));
                transferIdCounter++;
            }
        }

        // generating notice number and add to common RPT fields
        String noticeNumber = getNAVCodeFromIUVGenerator(commonRPTFieldsDTO.getCreditorInstitutionId());

        // mapping of payment option
        Long amount = commonRPTFieldsDTO.getRpts().stream()
                .map(rptContentDTO -> rptContentDTO.getRpt().getTransferData().getTotalAmount())
                .reduce(BigDecimal.valueOf(0L), BigDecimal::add)
                .longValue() * 100;
        PaymentOptionModelDto paymentOption = mapper.toPaymentOption(firstRPTContentDTO);
        paymentOption.setNav(noticeNumber);
        paymentOption.setAmount(amount);
        paymentOption.setTransfer(transfers);

        // mapping of payment position
        PaymentPositionModelDto paymentPosition = mapper.toPaymentPosition(commonRPTFieldsDTO);
        paymentPosition.setIupd(calculateIUPD(commonRPTFieldsDTO.getCreditorInstitutionId()));
        paymentPosition.setPaymentOption(List.of(paymentOption));

        // update payment notices to be used for communication with Checkout
        commonRPTFieldsDTO.getPaymentNotices().add(PaymentNoticeContentDTO.builder()
                .noticeNumber(noticeNumber)
                .fiscalCode(firstRPTContentDTO.getRpt().getDomain().getDomainId())
                .amount(amount)
                .build());

        return List.of(paymentPosition);
    }

    private List<PaymentPositionModelDto> extractPaymentPositionsForNonMultibeneficiary(CommonRPTFieldsDTO commonRPTFieldsDTO) {
        List<PaymentPositionModelDto> paymentPositions = new LinkedList<>();

        List<PaymentNoticeContentDTO> paymentNotices = commonRPTFieldsDTO.getPaymentNotices();

        for (RPTContentDTO rptContentDTO : commonRPTFieldsDTO.getRpts()) {

            PaymentRequestDTO paymentRequestDTO = rptContentDTO.getRpt();

            // mapping of transfers
            int transferIdCounter = 1;
            List<TransferModelDto> transfers = new ArrayList<>();
            for (TransferDTO transferDTO : paymentRequestDTO.getTransferData().getTransfer()) {

                transfers.add(extractPaymentOptionTransfer(transferDTO, null, transferIdCounter));
                transferIdCounter++;
            }

            // generating notice number and add to common RPT fields
            String noticeNumber = getNAVCodeFromIUVGenerator(commonRPTFieldsDTO.getCreditorInstitutionId());

            // mapping of payment option
            Long amount = paymentRequestDTO.getTransferData().getTotalAmount().longValue() * 100;
            PaymentOptionModelDto paymentOption = mapper.toPaymentOption(rptContentDTO);
            paymentOption.setAmount(amount);
            paymentOption.setNav(noticeNumber);
            paymentOption.setTransfer(transfers);

            // mapping of payment position
            PaymentPositionModelDto paymentPosition = mapper.toPaymentPosition(commonRPTFieldsDTO);
            paymentPosition.setIupd(calculateIUPD(commonRPTFieldsDTO.getCreditorInstitutionId()));
            paymentPosition.setPaymentOption(List.of(paymentOption));
            paymentPositions.add(paymentPosition);

            // update payment notices to be used for communication with Checkout
            paymentNotices.add(PaymentNoticeContentDTO.builder()
                    .noticeNumber(noticeNumber)
                    .fiscalCode(paymentRequestDTO.getDomain().getDomainId())
                    .amount(amount)
                    .build());
        }
        return paymentPositions;
    }

    private String getNAVCodeFromIUVGenerator(String creditorInstitutionCode) {

        String navCode;
        try {
            IuvGenerationModelDto request = new IuvGenerationModelDto();
            request.setAuxDigit(this.auxDigit);
            request.setSegregationCode(this.segregationCode);

            // communicating with IUV Generator service in order to retrieve response
            IuvGeneratorApiApi apiInstance = new IuvGeneratorApiApi(iuvGeneratorClient);
            IuvGenerationModelResponseDto response = apiInstance.generateIUV(creditorInstitutionCode, request);

            navCode = response.getIuv();
        } catch (RestClientException e) {
            throw new AppException(AppErrorCodeMessageEnum.CLIENT_IUVGENERATOR, e.getMessage());
        }
        return navCode;
    }

    private TransferModelDto extractPaymentOptionTransfer(TransferDTO transferDTO, String organizationFiscalCode, int transferIdCounter) {

        // definition of standard transfer metadata
        TransferMetadataModelDto transferMetadata = new TransferMetadataModelDto();
        transferMetadata.setKey("DatiSpecificiRiscossione");
        transferMetadata.setValue(transferDTO.getCategory());

        // common definition for the transfer
        TransferModelDto transfer = new TransferModelDto();
        transfer.setIdTransfer(TransferModelDto.IdTransferEnum.fromValue(String.valueOf(transferIdCounter)));
        transfer.setAmount(transferDTO.getAmount().longValue() * 100);
        transfer.setRemittanceInformation(transferDTO.getRemittanceInformation());
        transfer.setCategory(getTaxonomy(transferDTO));
        transfer.setTransferMetadata(List.of(transferMetadata));

        /*
        If digital stamp exists, it is a special transfer that does not require IBANs.
        If digital stamp doesn't exists, it is a common transfer that needs IBAN and needs the explicit setting of the organization fiscal code.
        */
        DigitalStampDTO digitalStampDTO = transferDTO.getDigitalStamp();
        if (digitalStampDTO != null) {

            transfer.setStamp(mapper.toStamp(digitalStampDTO));
        } else {

            String iban = transferDTO.getCreditIban();
            if (iban == null) {
                throw new AppException(AppErrorCodeMessageEnum.VALIDATION_INVALID_IBANS);
            }
            transfer.setIban(iban);
            transfer.setPostalIban(isPostalIBAN(iban) ? iban : null);
            transfer.setOrganizationFiscalCode(organizationFiscalCode);
        }

        return transfer;
    }

    private String getTaxonomy(TransferDTO transferDTO) {
        String taxonomy = transferDTO.getCategory();
        Matcher matcher = taxonomyPattern.matcher(taxonomy);
        if (matcher.find()) {
            taxonomy = matcher.group(1);
        }
        return taxonomy;
    }

    private boolean isPostalIBAN(String iban) {
        return iban != null && iban.substring(5, 10).equals(posteItalianeABICode);
    }

    private String calculateIUPD(String creditorInstitutionBroker) {
        return "wisp_" + creditorInstitutionBroker + "_" + UUID.randomUUID();
    }
}
