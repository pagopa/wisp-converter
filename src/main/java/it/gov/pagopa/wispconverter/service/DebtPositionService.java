package it.gov.pagopa.wispconverter.service;

import feign.FeignException;
import it.gov.pagopa.wispconverter.client.gpd.GPDClient;
import it.gov.pagopa.wispconverter.client.gpd.model.PaymentOption;
import it.gov.pagopa.wispconverter.client.gpd.model.PaymentPosition;
import it.gov.pagopa.wispconverter.client.gpd.model.Transfer;
import it.gov.pagopa.wispconverter.client.gpd.model.TransferMetadata;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.service.mapper.DebtPositionMapper;
import it.gov.pagopa.wispconverter.service.model.CommonRPTFieldsDTO;
import it.gov.pagopa.wispconverter.service.model.DigitalStampDTO;
import it.gov.pagopa.wispconverter.service.model.RPTContentDTO;
import it.gov.pagopa.wispconverter.service.model.TransferDTO;
import it.gov.pagopa.wispconverter.service.model.paymentrequest.PaymentRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class DebtPositionService {

    private final GPDClient gpdClient;

    private final DebtPositionMapper mapper;

    private final Pattern taxonomyPattern = Pattern.compile("([^/]+/[^/]+)/?");

    @Value("${wisp-converter.poste-italiane.abi-code}")
    private String posteItalianeABICode;

    public void createDebtPositions(CommonRPTFieldsDTO rptContentDTOs) {

        try {

            // converting RPTs in single payment position
            PaymentPosition paymentPosition = extractPaymentPosition(rptContentDTOs);

            // communicating with GPD-core service in order to execute the operation
            this.gpdClient.executeCreation(rptContentDTOs.getCreditorInstitutionId(), paymentPosition);

        } catch (FeignException e) {
            throw new AppException(e, AppErrorCodeMessageEnum.CLIENT_GPD, e.status(), e.getMessage());
        }
    }

    private PaymentPosition extractPaymentPosition(CommonRPTFieldsDTO commonRPTFieldsDTO) {

        //
        PaymentOption paymentOption = mapper.toPaymentOption(commonRPTFieldsDTO);
        paymentOption.setAmount(commonRPTFieldsDTO.getRpts().stream()
                .map(rptContentDTO -> rptContentDTO.getRpt().getTransferData().getTotalAmount())
                .reduce(BigDecimal.valueOf(0L), BigDecimal::add)
                .longValue() * 100);
        paymentOption.setTransfer(extractPaymentOptionTransfers(commonRPTFieldsDTO));
        //
        PaymentPosition paymentPosition = mapper.toPaymentPosition(commonRPTFieldsDTO);
        paymentPosition.setPaymentOption(List.of(paymentOption));
        return paymentPosition;
    }

    private List<Transfer> extractPaymentOptionTransfers(CommonRPTFieldsDTO commonRPTFieldsDTO) {

        int transferIdCounter = 0;

        List<Transfer> transfers = new ArrayList<>();
        for (RPTContentDTO rptContentDTO : commonRPTFieldsDTO.getRpts()) {

            PaymentRequestDTO rpt = rptContentDTO.getRpt();


            // organization fiscal code setting
            String organizationFiscalCode = getOrganizationFiscalCode(commonRPTFieldsDTO, rpt);

            for (TransferDTO transferDTO : rpt.getTransferData().getTransfer()) {

                // IBAN settings
                String iban = transferDTO.getCreditIban();
                //String iban = transferDTO.getDebitIban();;
                String postalIban = null;
                if (isPostalIBAN(iban)) {
                    postalIban = iban;
                    iban = null;
                }

                // common definition for the transfer
                Transfer transfer = Transfer.builder()
                        .idTransfer(String.valueOf(++transferIdCounter))  // mandatory
                        .amount(transferDTO.getAmount().longValue() * 100)  // mandatory
                        .remittanceInformation(transferDTO.getRemittanceInformation()) // mandatory
                        .category(getTaxonomy(transferDTO))  // mandatory
                        .transferMetadata(List.of(
                                TransferMetadata.builder()
                                        .key("DatiSpecificiRiscossione")
                                        .value(transferDTO.getCategory())
                                        .build()
                        ))
                        .build();

                /* if digital stamp exists, it is a special transfer that does not require IBANs */
                DigitalStampDTO digitalStampDTO = transferDTO.getDigitalStamp();
                if (digitalStampDTO != null) {
                    transfer.setStamp(mapper.toStamp(digitalStampDTO));
                }
                /*
                if digital stamp don't exists, it is a common transfer that needs IBAN and, if the cart is multibeneficiary,
                needs the explicit setting of the organization fiscal code.
                */
                else {
                    checkIBANValidity(iban, postalIban);
                    transfer.setIban(iban);
                    transfer.setPostalIban(postalIban);
                    transfer.setOrganizationFiscalCode(organizationFiscalCode);
                }

                transfers.add(transfer);
            }
        }

        return transfers;
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

    private void checkIBANValidity(String iban, String postalIban) throws AppException {
        if (iban == null && postalIban == null) {
            throw new AppException(AppErrorCodeMessageEnum.LOGIC_);
        }
    }

    private String getOrganizationFiscalCode(CommonRPTFieldsDTO commonRPTFieldsDTO, PaymentRequestDTO rpt) {
        return Boolean.TRUE.equals(commonRPTFieldsDTO.getIsMultibeneficiary()) ? rpt.getDomain().getDomainId() : null;
    }
}
