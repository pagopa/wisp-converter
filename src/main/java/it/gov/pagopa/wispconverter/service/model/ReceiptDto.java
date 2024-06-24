package it.gov.pagopa.wispconverter.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReceiptDto {

    private String paymentToken;
    private String fiscalCode;
    private String noticeNumber;

    @Override
    public String toString() {
        return "{\"paymentToken\": \"" + paymentToken + "\", \"fiscalCode\": \"" + fiscalCode + "\", \"noticeNumber\": \"" + noticeNumber + "\"}";
    }
}
