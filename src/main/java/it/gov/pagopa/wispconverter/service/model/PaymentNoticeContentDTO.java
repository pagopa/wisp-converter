package it.gov.pagopa.wispconverter.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentNoticeContentDTO {

    private String iuv;
    private String noticeNumber;
    private String fiscalCode;
    private String companyName;
    private String description;
    private Long amount;
}
