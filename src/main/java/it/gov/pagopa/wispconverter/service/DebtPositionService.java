package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.gen.wispconverter.client.gpd.api.DebtPositionsApiApi;
import it.gov.pagopa.gen.wispconverter.client.gpd.model.*;
import it.gov.pagopa.gen.wispconverter.client.iuvgenerator.api.GenerationApi;
import it.gov.pagopa.gen.wispconverter.client.iuvgenerator.model.IUVGenerationResponseDto;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.model.enumz.WorkflowStatus;
import it.gov.pagopa.wispconverter.service.mapper.DebtPositionMapper;
import it.gov.pagopa.wispconverter.service.mapper.DebtPositionUpdateMapper;
import it.gov.pagopa.wispconverter.service.model.DigitalStampDTO;
import it.gov.pagopa.wispconverter.service.model.TransferDTO;
import it.gov.pagopa.wispconverter.service.model.paymentrequest.PaymentRequestDTO;
import it.gov.pagopa.wispconverter.service.model.session.*;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import it.gov.pagopa.wispconverter.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DebtPositionService {

    private static final String REST_CLIENT_LOG_STRING = "RestClientException ERROR [%s] - %s";

    private final it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient gpdClient;

    private final it.gov.pagopa.gen.wispconverter.client.iuvgenerator.invoker.ApiClient iuvGeneratorClient;

    private final ReService reService;

    private final ReceiptService receiptService;

    private final DecouplerService decouplerService;

    private final ConfigCacheService configCacheService;

    private final DebtPositionMapper mapper;

    private final DebtPositionUpdateMapper mapperForUpdate;

    private final Pattern taxonomyPattern = Pattern.compile("^([^/]++/[^/]++)/?$");

    @Value("${wisp-converter.poste-italiane.abi-code}")
    private String posteItalianeABICode;

    @Value("${wisp-converter.aux-digit}")
    private Integer auxDigit;

    @Value("${wisp-converter.segregation-code}")
    private Integer segregationCode;

    @Value("${wisp-converter.payment-position-valid-status}")
    private Set<String> paymentPositionValidStatuses;

    @Value("${wisp-converter.station-in-gpd.partial-path}")
    private String stationInGpdPartialPath;

    @Value("${wisp-converter.re-tracing.internal.payment-position-analysis.enabled}")
    private Boolean isTracingOnREEnabled;

    public void createDebtPositions(SessionDataDTO sessionData) {

        // initialize standard data
        List<String> iuvToSaveInBulkOperation = new LinkedList<>();

        // instantiating client for GPD-core service
        DebtPositionsApiApi gpdClientInstance = new DebtPositionsApiApi(gpdClient);

        // extracting payment position from the session data, correctly clustering RPT if multibeneficiary or not
        List<PaymentPositionModelDto> extractedPaymentPositions = extractPaymentPositionsFromSessionData(sessionData);

        /*
          Check if each extracted payment position exists or not in GPD.
          If exists, a check on payment position status will be made and if it is valid, it will be updated.
          If it is not valid, an exception will be thrown.
         */
        for (PaymentPositionModelDto extractedPaymentPosition : extractedPaymentPositions) {

            // extract IUV from analyzed payment position (or throw an exception if not existing)
            String iuv = CommonUtility.getSinglePaymentOption(extractedPaymentPosition).getIuv();
            List<RPTContentDTO> rpts = sessionData.getRPTByIUV(iuv);
            String creditorInstitutionId = rpts.stream().map(rpt -> rpt.getRpt().getDomain().getDomainId()).findFirst().orElse(null);

            /*
              Using the extracted IUV, check if a payment position already exists in GPD.
              In any case, returns a status that will be used for evaluate each case, or eventually throw an exception if something went wrong.
             */
            MDC.put(Constants.MDC_IUV, iuv);
            MDC.put(Constants.MDC_DOMAIN_ID, creditorInstitutionId);
            Pair<PaymentPositionExistence, Optional<PaymentPositionModelBaseResponseDto>> response = checkPaymentPositionExistenceInGPD(gpdClientInstance, creditorInstitutionId, iuv);

            switch (response.getFirst()) {
                case EXISTS_INVALID ->
                        handleInvalidPaymentPositions(gpdClientInstance, sessionData, response.getSecond().orElse(null), creditorInstitutionId);
                case EXISTS_VALID ->
                        handleValidPaymentPosition(gpdClientInstance, sessionData, extractedPaymentPosition, response.getSecond().orElse(null), iuv, creditorInstitutionId);
                case NOT_EXISTS ->
                        handleNewPaymentPosition(sessionData, extractedPaymentPosition, iuvToSaveInBulkOperation, iuv, creditorInstitutionId);
            }
        }
        MDC.remove(Constants.MDC_IUV);
        MDC.remove(Constants.MDC_DOMAIN_ID);

        // executing the bulk insert of that payment position that were not available in GPD
        handlePaymentPositionInsertion(gpdClientInstance, sessionData, extractedPaymentPositions, iuvToSaveInBulkOperation);
    }

    private List<PaymentPositionModelDto> extractPaymentPositionsFromSessionData(SessionDataDTO sessionData) {

        List<PaymentPositionModelDto> paymentPositions;
        if (Boolean.TRUE.equals(sessionData.getCommonFields().getIsMultibeneficiary())) {
            paymentPositions = extractPaymentPositionsForMultiBeneficiary(sessionData);
        } else {
            paymentPositions = extractPaymentPositionsForNonMultiBeneficiary(sessionData);
        }
        return paymentPositions;
    }

    private List<PaymentPositionModelDto> extractPaymentPositionsForMultiBeneficiary(SessionDataDTO sessionData) {

        // if the number of RPT is lower than two, then throw an exception for invalid multibeneficiary cart
        if (sessionData.getNumberOfRPT() < 2) {
            throw new AppException(AppErrorCodeMessageEnum.VALIDATION_INVALID_MULTIBENEFICIARY_CART);
        }

        // execute the mapping of the transfer from all RPTs in session data
        List<TransferModelDto> transfers = new ArrayList<>();
        int transferIdCounter = 1;
        for (RPTContentDTO rptContent : sessionData.getAllRPTs()) {

            // extracting the payment from analyzed RPT in order to generate the transfers.
            PaymentRequestDTO paymentExtractedFromRPT = rptContent.getRpt();

            /*
              Generating transfer for the future GPD's payment option.
              All the transfers extracted from all the RPTs will be added in a single payment option.
              From NdP, we know that will be at most five transfers.
             */
            for (TransferDTO transfer : paymentExtractedFromRPT.getTransferData().getTransfer()) {

                String domainId = paymentExtractedFromRPT.getDomain().getDomainId();
                transfers.add(extractTransferForPaymentOption(transfer, domainId, transferIdCounter));
                transferIdCounter++;
            }
        }

        // retrieving the first RPT: this will be used as reference for the single payment option to be generated for GPD
        RPTContentDTO firstRPTContent = sessionData.getFirstRPT();
        PaymentOptionModelDto paymentOption = mapper.toPaymentOption(firstRPTContent);

        // updating the newly generated payment option with the data related to the extracted transfers
        Long amount = (long) (sessionData.getAllRPTs().stream()
                .map(rptContentDTO -> rptContentDTO.getRpt().getTransferData().getTotalAmount())
                .reduce(BigDecimal.valueOf(0L), BigDecimal::add)
                .multiply(Constants.AMOUNT_SCALE_INCREMENT)
                .doubleValue());
        paymentOption.setAmount(amount);
        paymentOption.setTransfer(transfers);
        paymentOption.setDescription(firstRPTContent.getRpt().getTransferData().getTransfer().get(0).getRemittanceInformation());
        paymentOption.setPaymentOptionMetadata(extractPaymentOptionMetadata(sessionData.getCommonFields()));

        // finally, generate the payment position and add the payment option
        String creditorInstitutionId = sessionData.getCommonFields().getCreditorInstitutionId();
        PaymentPositionModelDto paymentPosition = mapper.toPaymentPosition(sessionData);
        paymentPosition.setIupd(generateIUPD(creditorInstitutionId));
        paymentPosition.setCompanyName(configCacheService.getCreditorInstitutionNameFromCache(creditorInstitutionId));
        paymentPosition.setPaymentOption(List.of(paymentOption));

        // update payment notices to be used for communication with Checkout
        sessionData.addPaymentNotice(PaymentNoticeContentDTO.builder()
                .iuv(paymentOption.getIuv())
                .fiscalCode(firstRPTContent.getRpt().getDomain().getDomainId())
                .ccp(firstRPTContent.getCcp())
                .companyName(firstRPTContent.getRpt().getPayeeInstitution().getName())
                .description(firstRPTContent.getRpt().getTransferData().getTransfer().get(0).getRemittanceInformation())
                .amount(amount)
                .build());

        return List.of(paymentPosition);
    }

    private List<PaymentPositionModelDto> extractPaymentPositionsForNonMultiBeneficiary(SessionDataDTO sessionData) {

        List<PaymentPositionModelDto> paymentPositions = new LinkedList<>();

        // execute the mapping of the transfer from all RPTs in session data
        for (RPTContentDTO rptContent : sessionData.getAllRPTs()) {

            // extracting the payment from analyzed RPT in order to generate the transfers.
            PaymentRequestDTO paymentExtractedFromRPT = rptContent.getRpt();
            int transferIdCounter = 1;

            /*
              Generating transfer for the future GPD's payment option.
              All the transfers extracted from all the RPTs will be added in a single payment option.
              From NdP, we know that will be at most five transfers.
             */
            List<TransferModelDto> transfers = new ArrayList<>();
            for (TransferDTO transfer : paymentExtractedFromRPT.getTransferData().getTransfer()) {

                String domainId = paymentExtractedFromRPT.getDomain().getDomainId();
                transfers.add(extractTransferForPaymentOption(transfer, domainId, transferIdCounter));
                transferIdCounter++;
            }

            // generate a single payment option from the single RPT with the data related to the extracted transfers
            Long amount = (long) (paymentExtractedFromRPT.getTransferData().getTotalAmount().multiply(Constants.AMOUNT_SCALE_INCREMENT).doubleValue());
            PaymentOptionModelDto paymentOption = mapper.toPaymentOption(rptContent);
            paymentOption.setAmount(amount);
            paymentOption.setTransfer(transfers);
            paymentOption.setDescription(paymentExtractedFromRPT.getTransferData().getTransfer().get(0).getRemittanceInformation());
            paymentOption.setPaymentOptionMetadata(extractPaymentOptionMetadata(sessionData.getCommonFields()));

            // finally, generate the payment position and add the payment option
            String creditorInstitutionId = paymentExtractedFromRPT.getDomain().getDomainId();
            PaymentPositionModelDto paymentPosition = mapper.toPaymentPosition(rptContent);
            paymentPosition.setIupd(generateIUPD(creditorInstitutionId));
            paymentPosition.setCompanyName(configCacheService.getCreditorInstitutionNameFromCache(creditorInstitutionId));
            paymentPosition.setPaymentOption(List.of(paymentOption));
            paymentPositions.add(paymentPosition);

            // update payment notices to be used for communication with Checkout
            sessionData.addPaymentNotice(PaymentNoticeContentDTO.builder()
                    .iuv(paymentOption.getIuv())
                    .fiscalCode(paymentExtractedFromRPT.getDomain().getDomainId())
                    .ccp(paymentExtractedFromRPT.getTransferData().getCcp())
                    .companyName(paymentExtractedFromRPT.getPayeeInstitution().getName())
                    .description(paymentExtractedFromRPT.getTransferData().getTransfer().get(0).getRemittanceInformation())
                    .amount(amount)
                    .build());
        }
        return paymentPositions;
    }

    private TransferModelDto extractTransferForPaymentOption(TransferDTO transferDTO, String creditorInstitutionId, int transferIdCounter) {

        // setting the default metadata for this transfer
        List<TransferMetadataModelDto> transferMetadata = new ArrayList<>();
        TransferMetadataModelDto transferPaymentReasonMetadata = new TransferMetadataModelDto();
        transferPaymentReasonMetadata.setKey("DatiSpecificiRiscossione");
        transferPaymentReasonMetadata.setValue(transferDTO.getCategory());
        transferMetadata.add(transferPaymentReasonMetadata);

        // populating the transfer with the data extracted from RPT
        TransferModelDto transfer = new TransferModelDto();
        transfer.setIdTransfer(TransferModelDto.IdTransferEnum.fromValue(String.valueOf(transferIdCounter)));
        transfer.setAmount((long) (transferDTO.getAmount().multiply(Constants.AMOUNT_SCALE_INCREMENT).doubleValue()));
        transfer.setRemittanceInformation(transferDTO.getRemittanceInformation());
        transfer.setCategory(getTaxonomy(transferDTO));
        transfer.setTransferMetadata(transferMetadata);

        // If digital stamp exists, it is a special transfer that does not require IBANs.
        DigitalStampDTO digitalStampDTO = transferDTO.getDigitalStamp();
        if (digitalStampDTO != null) {
            transfer.setStamp(mapper.toStamp(digitalStampDTO));
        }

        // If digital stamp doesn't exist, it is a common transfer that needs IBAN and needs the explicit setting of the organization fiscal code.
        else {

            // if the IBAN of the payee is not set, then throw an exception because the payment is invalid
            String iban = transferDTO.getCreditIban();
            if (iban == null) {
                throw new AppException(AppErrorCodeMessageEnum.VALIDATION_INVALID_IBANS);
            }

            // also set the field 'postalIban' only if the IBAN refers to a Poste Italiane's IBAN.
            transfer.setIban(iban);
            transfer.setPostalIban(isPostalIBAN(iban) ? iban : null);
            transfer.setOrganizationFiscalCode(creditorInstitutionId);
        }

        return transfer;
    }

    private Pair<PaymentPositionExistence, Optional<PaymentPositionModelBaseResponseDto>> checkPaymentPositionExistenceInGPD(DebtPositionsApiApi gpdClientInstance, String creditorInstitutionId, String iuv) {

        // initialize components for required response
        PaymentPositionExistence status;
        Optional<PaymentPositionModelBaseResponseDto> body = Optional.empty();

        try {

            // communicate with GPD in order to retrieve the debt position
            PaymentPositionModelBaseResponseDto debtPosition = gpdClientInstance.getDebtPositionByIUV(creditorInstitutionId, iuv, MDC.get(Constants.MDC_OPERATION_ID));

            /*
              If status code is 200, a check on payment position status is required in order to choose the next step.
              The absence of response body is an error, so throw an exception.
             */
            if (debtPosition == null || debtPosition.getStatus() == null) {
                throw new AppException(AppErrorCodeMessageEnum.PAYMENT_POSITION_IN_INCONSISTENT_STATE, iuv);
            }

            /*
              If the payment position is in a state from which can be paid, the returned response is a positive state
              and a request body associated. If not, the returned state is negative and must provide dedicated action.
             */
            if (paymentPositionValidStatuses.contains(debtPosition.getStatus().getValue())) {
                status = PaymentPositionExistence.EXISTS_VALID;
            } else {
                status = PaymentPositionExistence.EXISTS_INVALID;
            }
            body = Optional.of(debtPosition);

        } catch (Exception e) {

            // if status code is 404, no valid payment position exists. So, a new one can be freely created
            if (e.getCause() instanceof HttpClientErrorException clientError && clientError.getStatusCode().value() == 404) {
                status = PaymentPositionExistence.NOT_EXISTS;
            }
            // any other status code is an error, so throw an exception
            else {
                throw new AppException(e, AppErrorCodeMessageEnum.CLIENT_GPD, String.format("Unable to read payment position with IUV [%s].", iuv));
            }
        }

        return Pair.of(status, body);
    }

    private void handleInvalidPaymentPositions(DebtPositionsApiApi gpdClientInstance, SessionDataDTO sessionData, PaymentPositionModelBaseResponseDto retrievedPaymentPosition, String creditorInstitutionId) {

        // check if retrieved payment is correct, otherwise throw an exception
        String iuvFromRetrievedPaymentPosition = CommonUtility.getSinglePaymentOption(retrievedPaymentPosition).getIuv();

        /*
           Generate a KO receipt (RT-) for the extracted payment positions.
           In this case, the operation is made on ALL the payment position because the payment position insert operation
           must be executed atomically.
         */
        for (PaymentNoticeContentDTO paymentNotice : sessionData.getAllPaymentNotices()) {

            String iuv = paymentNotice.getIuv();
            // communicate with GPD in order to retrieve the debt position
            PaymentPositionModelBaseResponseDto paymentPosition = retrievedPaymentPosition;
            if (!iuv.equals(iuvFromRetrievedPaymentPosition)) {
                paymentPosition = gpdClientInstance.getDebtPositionByIUV(creditorInstitutionId, iuv, MDC.get(Constants.MDC_REQUEST_ID));
                if (paymentPosition.getPaymentOption() == null || paymentPosition.getPaymentOption().isEmpty()) {
                    throw new AppException(AppErrorCodeMessageEnum.PAYMENT_OPTION_NOT_EXTRACTABLE, "Empty payment option");
                }
            }

            // update the payment notice, setting the NAV code from the retrieved existing payment position
            paymentNotice.setNoticeNumber(CommonUtility.getSinglePaymentOption(paymentPosition).getNav());

            // save event in RE for trace the error due to invalid payment position status
            generateREForInvalidPaymentPosition(sessionData, iuv);
        }

        // finally, throw an exception for notify the error, including all the IUVs
        String invalidIuvs = sessionData.getAllPaymentNotices().stream()
                .map(PaymentNoticeContentDTO::getIuv)
                .collect(Collectors.joining());
        throw new AppException(AppErrorCodeMessageEnum.PAYMENT_POSITION_NOT_IN_PAYABLE_STATE, invalidIuvs);
    }

    private void handleValidPaymentPosition(DebtPositionsApiApi gpdClientInstance, SessionDataDTO sessionData, PaymentPositionModelDto extractedPaymentPosition, PaymentPositionModelBaseResponseDto paymentPositionFromGPD, String iuv, String creditorInstitutionId) {

        try {
            // validate the station, checking if exists one with the required segregation code and, if is onboarded on GPD, has the correct primitive version
            CommonUtility.checkStationValidity(configCacheService, sessionData, creditorInstitutionId, CommonUtility.getSinglePaymentOption(paymentPositionFromGPD).getNav(), stationInGpdPartialPath);

            // merge the information of extracted payment position with the data from existing payment position, retrieved from GPD
            PaymentPositionModelDto updatedPaymentPosition = updateExtractedPaymentPositionWithExistingData(iuv, sessionData, paymentPositionFromGPD, extractedPaymentPosition);

            // communicating with GPD service in order to update the existing payment position
            gpdClientInstance.updatePosition(creditorInstitutionId, updatedPaymentPosition.getIupd(), updatedPaymentPosition, MDC.get(Constants.MDC_REQUEST_ID), true);


        } catch (RestClientException e) {

            throw new AppException(AppErrorCodeMessageEnum.CLIENT_GPD, String.format(REST_CLIENT_LOG_STRING, e.getCause().getClass().getCanonicalName(), e.getMessage()));
        }
    }

    private PaymentPositionModelDto updateExtractedPaymentPositionWithExistingData(String iuv, SessionDataDTO sessionData, PaymentPositionModelBaseResponseDto paymentPositionFromGPD, PaymentPositionModelDto extractedPaymentPosition) {

        // executing a first mapping of the payment position using the one retrieved from GPD as blueprint
        PaymentPositionModelDto updatedPaymentPosition = mapperForUpdate.toPaymentPosition(paymentPositionFromGPD);

        // setting missing fields on payment position that was mapped from retrieved one
        updatedPaymentPosition.validityDate(null)
                .payStandIn(extractedPaymentPosition.getPayStandIn())
                .fiscalCode(extractedPaymentPosition.getFiscalCode())
                .fullName(extractedPaymentPosition.getFullName())
                .streetName(extractedPaymentPosition.getStreetName())
                .civicNumber(extractedPaymentPosition.getCivicNumber())
                .postalCode(extractedPaymentPosition.getPostalCode())
                .city(extractedPaymentPosition.getCity())
                .province(extractedPaymentPosition.getProvince())
                .region(extractedPaymentPosition.getRegion())
                .country(extractedPaymentPosition.getCountry())
                .email(extractedPaymentPosition.getEmail())
                .phone(extractedPaymentPosition.getPhone())
                .switchToExpired(false);

        if (updatedPaymentPosition.getPaymentOption() != null && extractedPaymentPosition.getPaymentOption() != null) {

            /*
               From the list of payment options of updated and extracted payment options, get the one that has the same
               IUV that is used in the evaluation. If no payment option is found, throw and exception.
             */
            PaymentOptionModelDto updatedPaymentOption = updatedPaymentPosition.getPaymentOption().stream()
                    .filter(paymentOption -> paymentOption.getIuv().equals(iuv))
                    .findFirst()
                    .orElseThrow(() -> new AppException(AppErrorCodeMessageEnum.PAYMENT_POSITION_NOT_VALID, iuv, "The IUV is not associated to a payment option in the payment position retrieved from GPD."));
            PaymentOptionModelDto extractedPaymentOption = extractedPaymentPosition.getPaymentOption().stream()
                    .filter(paymentOption -> paymentOption.getIuv().equals(iuv))
                    .findFirst()
                    .orElseThrow(() -> new AppException(AppErrorCodeMessageEnum.PAYMENT_POSITION_NOT_VALID, iuv, "The IUV is not associated to a payment option in the payment position generated from RPT"));

            // now, update the field on which is required to change info about the payment
            updatedPaymentOption.setAmount(extractedPaymentOption.getAmount());
            updatedPaymentOption.setDescription(extractedPaymentOption.getDescription());
            updatedPaymentOption.setTransfer(extractedPaymentOption.getTransfer());
            updatedPaymentOption.setDueDate(OffsetDateTime.now().plusDays(1));

            // finally, update the payment notice to be used for Checkout with the existing NAV code.
            sessionData.getPaymentNoticeByIUV(iuv).setNoticeNumber(updatedPaymentOption.getNav());
        }

        return updatedPaymentPosition;
    }

    private List<PaymentOptionMetadataModelDto> extractPaymentOptionMetadata(CommonFieldsDTO commonFields) {
        // if old-style fee code from Catalogo Dati Informativi is set, pass it as metadata
        List<PaymentOptionMetadataModelDto> metadata = new ArrayList<>();
        String feeCode = commonFields.getFeeCode();
        if (feeCode != null) {
            PaymentOptionMetadataModelDto feeCodeMetadata = new PaymentOptionMetadataModelDto();
            feeCodeMetadata.setKey("codiceConvenzione");
            feeCodeMetadata.setValue(feeCode);
            metadata.add(feeCodeMetadata);
        }
        return metadata;
    }

    private void handleNewPaymentPosition(SessionDataDTO sessionData, PaymentPositionModelDto extractedPaymentPosition, List<String> iuvToSaveInBulkOperation, String iuv, String creditorInstitutionId) {

        // generate a new NAV code calling IUVGenerator
        String nav = generateNavCodeFromIuvGenerator(creditorInstitutionId);
        if (!Constants.NODO_INVIA_CARRELLO_RPT.equals(MDC.get(Constants.MDC_PRIMITIVE))) {
            MDC.put(Constants.MDC_NOTICE_NUMBER, nav);
            MDC.put(Constants.MDC_IUV, iuv);
        }

        // update the payment option to be used in bulk insert with the newly generated NAV code
        List<PaymentOptionModelDto> paymentOptions = extractedPaymentPosition.getPaymentOption();
        if (paymentOptions == null || paymentOptions.isEmpty()) {
            throw new AppException(AppErrorCodeMessageEnum.PAYMENT_OPTION_NOT_EXTRACTABLE, "Empty payment option");
        }
        paymentOptions.get(0).setNav(nav);

        // update payment notices to be used for communication with Checkout, including the newly generated NAV
        sessionData.getPaymentNoticeByIUV(iuv).setNoticeNumber(nav);
        iuvToSaveInBulkOperation.add(iuv);
    }

    private String generateNavCodeFromIuvGenerator(String creditorInstitutionId) {

        String navCode;
        try {

            // generating request for IUVGenerator, explicitly setting AUX-Digit and segregation code
            it.gov.pagopa.gen.wispconverter.client.iuvgenerator.model.IUVGenerationRequestDto request = new it.gov.pagopa.gen.wispconverter.client.iuvgenerator.model.IUVGenerationRequestDto();
            request.setAuxDigit(this.auxDigit);
            request.setSegregationCode(this.segregationCode);

            // communicating with IUV Generator service in order to retrieve response
            GenerationApi iuvGenClientInstance = new it.gov.pagopa.gen.wispconverter.client.iuvgenerator.api.GenerationApi(iuvGeneratorClient);
            IUVGenerationResponseDto response = iuvGenClientInstance.generateIUV(creditorInstitutionId, request);

            // generating NAV code using standard AUX digit and the generated IUV code
            navCode = this.auxDigit + response.getIuv();

        } catch (RestClientException e) {
            throw new AppException(AppErrorCodeMessageEnum.CLIENT_IUVGENERATOR,
                    String.format(REST_CLIENT_LOG_STRING, e.getCause().getClass().getCanonicalName(), e.getMessage()));
        }
        return navCode;
    }

    private void handlePaymentPositionInsertion(DebtPositionsApiApi gpdClientInstance, SessionDataDTO sessionData, List<PaymentPositionModelDto> extractedPaymentPositions, List<String> iuvToSaveInBulkOperation) {

        // execute the handle if and only if there is at least one payment position to be added
        if (!iuvToSaveInBulkOperation.isEmpty()) {

            // clusterizing IUV codes by creditor institution
            Map<String, Set<String>> iuvClusteredByCI = new HashMap<>();
            for (String iuv : iuvToSaveInBulkOperation) {
                List<RPTContentDTO> rpts = sessionData.getRPTByIUV(iuv);
                if (!rpts.isEmpty()) {
                    String creditorInstitutionId = rpts.stream().map(rpt -> rpt.getRpt().getDomain().getDomainId()).findFirst().orElse("");
                    Set<String> cluster = iuvClusteredByCI.computeIfAbsent(creditorInstitutionId, k -> new HashSet<>());
                    cluster.add(iuv);
                }
            }

            try {
                for (Map.Entry<String, Set<String>> entry : iuvClusteredByCI.entrySet()) {

                    // extracting payment positions by creditor institution
                    String creditorInstitutionId = entry.getKey();
                    Set<String> iuvsForCreditorInstitution = entry.getValue();
                    List<PaymentPositionModelDto> paymentPositionsToCreateForCreditorInstitution = extractedPaymentPositions.stream()
                            .filter(pp -> iuvsForCreditorInstitution.contains(CommonUtility.getSinglePaymentOption(pp).getIuv()))
                            .toList();

                    // generate request and communicating with GPD-core service in order to execute the bulk insertion
                    it.gov.pagopa.gen.wispconverter.client.gpd.model.MultiplePaymentPositionModelDto multiplePaymentPositions = new MultiplePaymentPositionModelDto();
                    multiplePaymentPositions.setPaymentPositions(paymentPositionsToCreateForCreditorInstitution);

                    // communicating with GPD service in order to update the existing payment position
                    gpdClientInstance.createMultiplePositions(creditorInstitutionId, multiplePaymentPositions, MDC.get(Constants.MDC_REQUEST_ID), true);
                }

            } catch (RestClientException e) {
                throw new AppException(AppErrorCodeMessageEnum.CLIENT_GPD, String.format(REST_CLIENT_LOG_STRING, e.getCause().getClass().getCanonicalName(), e.getMessage()));
            }
        }
    }

    private String generateIUPD(String creditorInstitutionBroker) {
        return "wisp_" + creditorInstitutionBroker + "_" + UUID.randomUUID();
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


    private void generateREForInvalidPaymentPosition(SessionDataDTO sessionDataDTO, String iuv) {

        // creating event to be persisted for RE
        if (Boolean.TRUE.equals(isTracingOnREEnabled)) {
            PaymentNoticeContentDTO paymentNotice = sessionDataDTO.getPaymentNoticeByIUV(iuv);
            MDC.put(Constants.MDC_IUV, iuv);
            MDC.put(Constants.MDC_NOTICE_NUMBER, paymentNotice.getNoticeNumber());
            MDC.put(Constants.MDC_CCP, paymentNotice.getCcp());
            MDC.put(Constants.MDC_DOMAIN_ID, paymentNotice.getFiscalCode());
            reService.sendEvent(WorkflowStatus.CONVERSION_ERROR_SENDING_RT);
        }
    }

}
