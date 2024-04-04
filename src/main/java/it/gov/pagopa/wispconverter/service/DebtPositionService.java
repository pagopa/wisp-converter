package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.service.mapper.DebtPositionMapper;
import it.gov.pagopa.wispconverter.service.model.RPTContentDTO;
import it.gov.pagopa.wispconverter.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DebtPositionService {

    private final it.gov.pagopa.gpdclient.client.ApiClient gpdClient;

    private final DebtPositionMapper mapper;

    public void executeBulkCreation(List<RPTContentDTO> rptContentDTOs) {

        Map<String, List<RPTContentDTO>> paymentPositionsByDomain = rptContentDTOs.stream().collect(Collectors.groupingBy(RPTContentDTO::getIdDominio));

        paymentPositionsByDomain.forEach((idDominio, value) -> {
            List<it.gov.pagopa.gpdclient.model.PaymentPositionModelDto> paymentPositionModelDtoList = value.stream().map(mapper::toPaymentPosition).toList();

            // generating request
            it.gov.pagopa.gpdclient.model.MultiplePaymentPositionModelDto multiplePaymentPositionModelDto = new it.gov.pagopa.gpdclient.model.MultiplePaymentPositionModelDto();
            multiplePaymentPositionModelDto.setPaymentPositions(paymentPositionModelDtoList);

            String xRequestId = MDC.get(Constants.MDC_REQUEST_ID);
            Boolean toPublish = true;

            it.gov.pagopa.gpdclient.api.DebtPositionsApiApi apiInstance = new it.gov.pagopa.gpdclient.api.DebtPositionsApiApi(gpdClient);
            apiInstance.createMultiplePositions1(idDominio, multiplePaymentPositionModelDto, xRequestId, toPublish);
            //FIXME gestire errori di connessione
            //FIXME cosa succede se si spacca al secondo giro?
        });
    }


}
