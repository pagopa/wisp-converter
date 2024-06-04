package it.gov.pagopa.wispconverter.service.model.session;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.gov.pagopa.wispconverter.service.model.paymentrequest.PaymentRequestDTO;
import lombok.*;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class RPTContentDTO {

    private String iupd;
    private String iuv;
    private int index;
    private String ccp;
    private Boolean containsDigitalStamp;
    private PaymentRequestDTO rpt;
}
