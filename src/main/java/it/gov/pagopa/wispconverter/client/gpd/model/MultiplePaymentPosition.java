package it.gov.pagopa.wispconverter.client.gpd.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiplePaymentPosition implements Serializable {

    @Valid
    @NotEmpty
    private List<PaymentPosition> paymentPositions;
}
