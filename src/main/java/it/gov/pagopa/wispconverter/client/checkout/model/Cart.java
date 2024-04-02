package it.gov.pagopa.wispconverter.client.checkout.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Cart {

    private String idCart;
    private String emailNotice;
    private String stationId;
    private boolean allCCP;
    private ReturnURLs returnURLs;
    private List<PaymentNotice> paymentNotices;
}
