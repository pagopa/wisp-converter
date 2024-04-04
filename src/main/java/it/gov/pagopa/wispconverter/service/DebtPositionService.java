package it.gov.pagopa.wispconverter.service;

import feign.FeignException;
import it.gov.pagopa.wispconverter.client.gpd.GPDClient;
import it.gov.pagopa.wispconverter.client.gpd.model.*;
import it.gov.pagopa.wispconverter.client.iuvgenerator.IUVGeneratorClient;
import it.gov.pagopa.wispconverter.client.iuvgenerator.model.IUVGeneratorRequest;
import it.gov.pagopa.wispconverter.client.iuvgenerator.model.IUVGeneratorResponse;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.service.mapper.DebtPositionMapper;
import it.gov.pagopa.wispconverter.service.model.*;
import it.gov.pagopa.wispconverter.service.model.paymentrequest.PaymentRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    private final GPDClient gpdClient;

    private final IUVGeneratorClient iuvGeneratorClient;

    private final DebtPositionMapper mapper;

    private final Pattern taxonomyPattern = Pattern.compile("([^/]++/[^/]++)/?");

    @Value("${wisp-converter.poste-italiane.abi-code}")
    private String posteItalianeABICode;

    @Value("${wisp-converter.aux-digit}")
    private String auxDigit;

    @Value("${wisp-converter.segregation-code}")
    private String segregationCode;

    public void createDebtPositions(CommonRPTFieldsDTO rptContentDTOs) {

        try {

            // converting RPTs in single payment position
            MultiplePaymentPosition paymentPosition = extractPaymentPositions(rptContentDTOs);

            // communicating with GPD-core service in order to execute the operation
            this.gpdClient.executeBulkCreation(rptContentDTOs.getCreditorInstitutionId(), paymentPosition);

        } catch (FeignException e) {
            throw new AppException(e, AppErrorCodeMessageEnum.CLIENT_GPD, e.status(), e.getMessage());
        }
    }

    private MultiplePaymentPosition extractPaymentPositions(CommonRPTFieldsDTO commonRPTFieldsDTO) {

        List<PaymentPosition> paymentPositions;
        if (Boolean.TRUE.equals(commonRPTFieldsDTO.getIsMultibeneficiary())) {
            paymentPositions = extractPaymentPositionsForMultibeneficiary(commonRPTFieldsDTO);
        } else {
            paymentPositions = extractPaymentPositionsForNonMultibeneficiary(commonRPTFieldsDTO);
        }

        MultiplePaymentPosition multiplePaymentPosition = new MultiplePaymentPosition();
        multiplePaymentPosition.setPaymentPositions(paymentPositions);
        return multiplePaymentPosition;
    }

    private List<PaymentPosition> extractPaymentPositionsForMultibeneficiary(CommonRPTFieldsDTO commonRPTFieldsDTO) {

        if (commonRPTFieldsDTO.getRpts().size() < 2) {
            throw new AppException(AppErrorCodeMessageEnum.VALIDATION_INVALID_MULTIBENEFICIARY_CART);
        }
        RPTContentDTO firstRPTContentDTO = commonRPTFieldsDTO.getRpts().get(0);

        // mapping of transfers
        List<Transfer> transfers = new ArrayList<>();
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
        PaymentOption paymentOption = mapper.toPaymentOption(firstRPTContentDTO);
        paymentOption.setNav(noticeNumber);
        paymentOption.setAmount(amount);
        paymentOption.setTransfer(transfers);

        // mapping of payment position
        PaymentPosition paymentPosition = mapper.toPaymentPosition(commonRPTFieldsDTO);
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

    private List<PaymentPosition> extractPaymentPositionsForNonMultibeneficiary(CommonRPTFieldsDTO commonRPTFieldsDTO) {
        List<PaymentPosition> paymentPositions = new LinkedList<>();

        List<PaymentNoticeContentDTO> paymentNotices = commonRPTFieldsDTO.getPaymentNotices();

        for (RPTContentDTO rptContentDTO : commonRPTFieldsDTO.getRpts()) {

            PaymentRequestDTO paymentRequestDTO = rptContentDTO.getRpt();

            // mapping of transfers
            int transferIdCounter = 1;
            List<Transfer> transfers = new ArrayList<>();
            for (TransferDTO transferDTO : paymentRequestDTO.getTransferData().getTransfer()) {

                transfers.add(extractPaymentOptionTransfer(transferDTO, null, transferIdCounter));
                transferIdCounter++;
            }

            // generating notice number and add to common RPT fields
            String noticeNumber = getNAVCodeFromIUVGenerator(commonRPTFieldsDTO.getCreditorInstitutionId());

            // mapping of payment option
            Long amount = paymentRequestDTO.getTransferData().getTotalAmount().longValue() * 100;
            PaymentOption paymentOption = mapper.toPaymentOption(rptContentDTO);
            paymentOption.setAmount(amount);
            paymentOption.setNav(noticeNumber);
            paymentOption.setTransfer(transfers);

            // mapping of payment position
            PaymentPosition paymentPosition = mapper.toPaymentPosition(commonRPTFieldsDTO);
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


    private Transfer extractPaymentOptionTransfer(TransferDTO transferDTO, String organizationFiscalCode, int transferIdCounter) {

        // common definition for the transfer
        Transfer transfer = Transfer.builder()
                .idTransfer(String.valueOf(transferIdCounter))
                .amount(transferDTO.getAmount().longValue() * 100)
                .remittanceInformation(transferDTO.getRemittanceInformation())
                .category(getTaxonomy(transferDTO))
                .transferMetadata(List.of(
                        TransferMetadata.builder()
                                .key("DatiSpecificiRiscossione")
                                .value(transferDTO.getCategory())
                                .build()
                ))
                .build();

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
