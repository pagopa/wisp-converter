package it.gov.pagopa.wispconverter.client.checkout.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentNotice {

    private String noticeNumber;
    private String fiscalCode;
    private int amount;
    private String companyName;
    private String description;
}
