package it.gov.pagopa.wispconverter.service;

import feign.FeignException;
import it.gov.pagopa.wispconverter.client.GPDClient;
import it.gov.pagopa.wispconverter.exception.conversion.ConversionException;
import it.gov.pagopa.wispconverter.model.client.gpd.MultiplePaymentPosition;
import it.gov.pagopa.wispconverter.model.client.gpd.PaymentPosition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DebtPositionService {

    private final GPDClient gpdClient;

    public DebtPositionService(@Autowired GPDClient gpdClient) {
        this.gpdClient = gpdClient;
    }

    public MultiplePaymentPosition executeBulkCreation(String creditorInstitutionCode, List<PaymentPosition> paymentPositions) throws ConversionException {// generating request body
        // generating request
        MultiplePaymentPosition request = MultiplePaymentPosition.builder()
                .paymentPositions(paymentPositions)
                .build();
        try {
            // communicating with GPD-core service in order to execute the operation
            this.gpdClient.executeBulkCreation(creditorInstitutionCode, request);
        } catch (FeignException e) {
            throw new ConversionException("Unable to generate debt position with GPD-core service. An error occurred during communication with service:", e);
        }
        return request;
    }
}
