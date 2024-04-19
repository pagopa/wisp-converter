package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.gen.wispconverter.client.gpd.model.PaymentOptionModelDto;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.service.mapper.DebtPositionMapper;
import it.gov.pagopa.wispconverter.service.model.*;
import it.gov.pagopa.wispconverter.service.model.paymentrequest.PaymentRequestDTO;
import it.gov.pagopa.wispconverter.service.model.re.EntityStatusEnum;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ReUtil;
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

import static it.gov.pagopa.wispconverter.util.Constants.NODO_DEI_PAGAMENTI_SPC;

@Service
@Slf4j
@RequiredArgsConstructor
public class DebtPositionService {

    private final it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient gpdClient;

    private final it.gov.pagopa.gen.wispconverter.client.iuvgenerator.invoker.ApiClient iuvGeneratorClient;

    private final ReService reService;

    private final DebtPositionMapper mapper;

    private final Pattern taxonomyPattern = Pattern.compile("([^/]++/[^/]++)/?");

    @Value("${wisp-converter.poste-italiane.abi-code}")
    private String posteItalianeABICode;

    @Value("${wisp-converter.aux-digit}")
    private Integer auxDigit;

    @Value("${wisp-converter.segregation-code}")
    private Integer segregationCode;

    public void createDebtPositions(CommonRPTFieldsDTO rptContentDTOs) {

        try {
            // converting RPTs in single payment position
            it.gov.pagopa.gen.wispconverter.client.gpd.model.MultiplePaymentPositionModelDto multiplePaymentPositions = extractPaymentPositions(rptContentDTOs);

            // communicating with GPD-core service in order to execute the operation
            it.gov.pagopa.gen.wispconverter.client.gpd.api.DebtPositionsApiApi apiInstance = new it.gov.pagopa.gen.wispconverter.client.gpd.api.DebtPositionsApiApi(gpdClient);
            apiInstance.createMultiplePositions1(rptContentDTOs.getCreditorInstitutionId(), multiplePaymentPositions, MDC.get(Constants.MDC_REQUEST_ID), true);

            // generate and save re events internal for change status
            multiplePaymentPositions.getPaymentPositions().forEach(paymentPositionModelDto -> reService.addRe(generateRE(paymentPositionModelDto, rptContentDTOs, EntityStatusEnum.PD_CREATA.name())));

        } catch (RestClientException e) {
            throw new AppException(AppErrorCodeMessageEnum.CLIENT_GPD,
                    String.format("RestClientException ERROR [%s] - %s", e.getCause().getClass().getCanonicalName(), e.getMessage()));
        }
    }

    private it.gov.pagopa.gen.wispconverter.client.gpd.model.MultiplePaymentPositionModelDto extractPaymentPositions(CommonRPTFieldsDTO commonRPTFieldsDTO) {

        String operationStatus;
        List<it.gov.pagopa.gen.wispconverter.client.gpd.model.PaymentPositionModelDto> paymentPositions;
        if (Boolean.TRUE.equals(commonRPTFieldsDTO.getIsMultibeneficiary())) {
            paymentPositions = extractPaymentPositionsForMultibeneficiary(commonRPTFieldsDTO);
            operationStatus = EntityStatusEnum.PD_MULTIBENEFICIARIO_ESTRATTA.name();
        } else {
            paymentPositions = extractPaymentPositionsForNonMultibeneficiary(commonRPTFieldsDTO);
            operationStatus = EntityStatusEnum.PD_NON_MULTIBENEFICIARIO_ESTRATTA.name();
        }

        it.gov.pagopa.gen.wispconverter.client.gpd.model.MultiplePaymentPositionModelDto multiplePaymentPosition = new it.gov.pagopa.gen.wispconverter.client.gpd.model.MultiplePaymentPositionModelDto();
        multiplePaymentPosition.setPaymentPositions(paymentPositions);

        // generate and save re events internal for change status
        multiplePaymentPosition.getPaymentPositions().forEach(paymentPositionModelDto -> reService.addRe(generateRE(paymentPositionModelDto, commonRPTFieldsDTO, operationStatus)));

        return multiplePaymentPosition;
    }

    private List<it.gov.pagopa.gen.wispconverter.client.gpd.model.PaymentPositionModelDto> extractPaymentPositionsForMultibeneficiary(CommonRPTFieldsDTO commonRPTFieldsDTO) {

        if (commonRPTFieldsDTO.getRpts().size() < 2) {
            throw new AppException(AppErrorCodeMessageEnum.VALIDATION_INVALID_MULTIBENEFICIARY_CART);
        }
        RPTContentDTO firstRPTContentDTO = commonRPTFieldsDTO.getRpts().get(0);

        // mapping of transfers
        List<it.gov.pagopa.gen.wispconverter.client.gpd.model.TransferModelDto> transfers = new ArrayList<>();
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
        it.gov.pagopa.gen.wispconverter.client.gpd.model.PaymentOptionModelDto paymentOption = mapper.toPaymentOption(firstRPTContentDTO);
        paymentOption.setNav(noticeNumber);
        paymentOption.setAmount(amount);
        paymentOption.setTransfer(transfers);

        // mapping of payment position
        it.gov.pagopa.gen.wispconverter.client.gpd.model.PaymentPositionModelDto paymentPosition = mapper.toPaymentPosition(commonRPTFieldsDTO);
        paymentPosition.setIupd(calculateIUPD(commonRPTFieldsDTO.getCreditorInstitutionId()));
        paymentPosition.setPaymentOption(List.of(paymentOption));

        // update payment notices to be used for communication with Checkout
        commonRPTFieldsDTO.getPaymentNotices().add(PaymentNoticeContentDTO.builder()
                .iuv(paymentOption.getIuv())
                .noticeNumber(noticeNumber)
                .fiscalCode(firstRPTContentDTO.getRpt().getDomain().getDomainId())
                .companyName(firstRPTContentDTO.getRpt().getPayeeInstitution().getName())
                .description(firstRPTContentDTO.getRpt().getTransferData().getTransfer().get(0).getRemittanceInformation())
                .amount(amount)
                .build());

        return List.of(paymentPosition);
    }

    private List<it.gov.pagopa.gen.wispconverter.client.gpd.model.PaymentPositionModelDto> extractPaymentPositionsForNonMultibeneficiary(CommonRPTFieldsDTO commonRPTFieldsDTO) {
        List<it.gov.pagopa.gen.wispconverter.client.gpd.model.PaymentPositionModelDto> paymentPositions = new LinkedList<>();

        List<PaymentNoticeContentDTO> paymentNotices = commonRPTFieldsDTO.getPaymentNotices();

        for (RPTContentDTO rptContentDTO : commonRPTFieldsDTO.getRpts()) {

            PaymentRequestDTO paymentRequestDTO = rptContentDTO.getRpt();

            // mapping of transfers
            int transferIdCounter = 1;
            List<it.gov.pagopa.gen.wispconverter.client.gpd.model.TransferModelDto> transfers = new ArrayList<>();
            for (TransferDTO transferDTO : paymentRequestDTO.getTransferData().getTransfer()) {

                transfers.add(extractPaymentOptionTransfer(transferDTO, null, transferIdCounter));
                transferIdCounter++;
            }

            // generating notice number and add to common RPT fields
            String noticeNumber = getNAVCodeFromIUVGenerator(commonRPTFieldsDTO.getCreditorInstitutionId());

            // mapping of payment option
            Long amount = paymentRequestDTO.getTransferData().getTotalAmount().longValue() * 100;
            it.gov.pagopa.gen.wispconverter.client.gpd.model.PaymentOptionModelDto paymentOption = mapper.toPaymentOption(rptContentDTO);
            paymentOption.setAmount(amount);
            paymentOption.setNav(noticeNumber);
            paymentOption.setTransfer(transfers);

            // mapping of payment position
            it.gov.pagopa.gen.wispconverter.client.gpd.model.PaymentPositionModelDto paymentPosition = mapper.toPaymentPosition(commonRPTFieldsDTO);
            paymentPosition.setIupd(calculateIUPD(commonRPTFieldsDTO.getCreditorInstitutionId()));
            paymentPosition.setPaymentOption(List.of(paymentOption));
            paymentPositions.add(paymentPosition);

            // update payment notices to be used for communication with Checkout
            paymentNotices.add(PaymentNoticeContentDTO.builder()
                    .iuv(paymentOption.getIuv())
                    .noticeNumber(noticeNumber)
                    .fiscalCode(paymentRequestDTO.getDomain().getDomainId())
                    .companyName(paymentRequestDTO.getPayeeInstitution().getName())
                    .description(paymentRequestDTO.getTransferData().getTransfer().get(0).getRemittanceInformation())
                    .amount(amount)
                    .build());
        }
        return paymentPositions;
    }

    private String getNAVCodeFromIUVGenerator(String creditorInstitutionCode) {

        String navCode;
        try {
            it.gov.pagopa.gen.wispconverter.client.iuvgenerator.model.IUVGenerationRequestDto request = new it.gov.pagopa.gen.wispconverter.client.iuvgenerator.model.IUVGenerationRequestDto();
            request.setAuxDigit(this.auxDigit);
            request.setSegregationCode(this.segregationCode);

            // communicating with IUV Generator service in order to retrieve response
            it.gov.pagopa.gen.wispconverter.client.iuvgenerator.api.GenerationApi apiInstance = new it.gov.pagopa.gen.wispconverter.client.iuvgenerator.api.GenerationApi(iuvGeneratorClient);
            it.gov.pagopa.gen.wispconverter.client.iuvgenerator.model.IUVGenerationResponseDto response = apiInstance.generateIUV(creditorInstitutionCode, request);

            navCode = this.auxDigit + response.getIuv();
        } catch (RestClientException e) {
            throw new AppException(AppErrorCodeMessageEnum.CLIENT_IUVGENERATOR,
                    String.format("RestClientException ERROR [%s] - %s", e.getCause().getClass().getCanonicalName(), e.getMessage()));
        }
        return navCode;
    }

    private it.gov.pagopa.gen.wispconverter.client.gpd.model.TransferModelDto extractPaymentOptionTransfer(TransferDTO transferDTO, String organizationFiscalCode, int transferIdCounter) {

        // definition of standard transfer metadata
        it.gov.pagopa.gen.wispconverter.client.gpd.model.TransferMetadataModelDto transferMetadata = new it.gov.pagopa.gen.wispconverter.client.gpd.model.TransferMetadataModelDto();
        transferMetadata.setKey("DatiSpecificiRiscossione");
        transferMetadata.setValue(transferDTO.getCategory());

        // common definition for the transfer
        it.gov.pagopa.gen.wispconverter.client.gpd.model.TransferModelDto transfer = new it.gov.pagopa.gen.wispconverter.client.gpd.model.TransferModelDto();
        transfer.setIdTransfer(it.gov.pagopa.gen.wispconverter.client.gpd.model.TransferModelDto.IdTransferEnum.fromValue(String.valueOf(transferIdCounter)));
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

    private ReEventDto generateRE(it.gov.pagopa.gen.wispconverter.client.gpd.model.PaymentPositionModelDto paymentPosition, CommonRPTFieldsDTO commonRPTFieldsDTO, String status) {
        ReEventDto reEventDto = ReUtil.createBaseReInternal()
                .status(status)
                .erogatore(NODO_DEI_PAGAMENTI_SPC)
                .erogatoreDescr(NODO_DEI_PAGAMENTI_SPC)
                .sessionIdOriginal(MDC.get(Constants.MDC_SESSION_ID))
                .info(String.format("IUPD = [%s]", paymentPosition.getIupd()))
                .idDominio(commonRPTFieldsDTO.getCreditorInstitutionId())
                .stazione(commonRPTFieldsDTO.getStationId())
                .build();

        List<PaymentOptionModelDto> paymentOptions = paymentPosition.getPaymentOption();
        if (paymentOptions != null && !paymentOptions.isEmpty()) {
            PaymentOptionModelDto paymentOption = paymentOptions.get(0);
            reEventDto.setNoticeNumber(paymentOption.getNav());
            reEventDto.setIuv(paymentOption.getIuv());
        }

        return reEventDto;
    }
}
