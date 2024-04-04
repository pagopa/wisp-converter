package it.gov.pagopa.wispconverter.client.gpd.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
public class MultiplePaymentPosition implements Serializable {

    private List<PaymentPosition> paymentPositions;

}
