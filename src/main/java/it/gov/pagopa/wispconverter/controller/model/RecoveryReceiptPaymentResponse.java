package it.gov.pagopa.wispconverter.controller.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecoveryReceiptPaymentResponse {

    private String iuv;
    private String ccp;
    private String ci;
}
