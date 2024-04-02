package it.gov.pagopa.wispconverter.client.checkout.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnURLs {

    private String returnOkUrl;
    private String returnCancelUrl;
    private String returnErrorUrl;
}
