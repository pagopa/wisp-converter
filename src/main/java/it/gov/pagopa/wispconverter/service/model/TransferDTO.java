package it.gov.pagopa.wispconverter.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferDTO {
    protected BigDecimal amount;
    protected BigDecimal fee;
    protected String creditIban;
    protected String creditBic;
    protected String supportIban;
    protected String supportBic;
    protected String payerCredentials;
    protected String remittanceInformation;
    protected String category;
    protected DigitalStampDTO digitalStamp;
}
