package it.gov.pagopa.wispconverter.client.checkout.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReturnURLs {

    private String returnOkUrl;
    private String returnCancelUrl;
    private String returnErrorUrl;
}
