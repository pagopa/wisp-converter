package it.gov.pagopa.wispconverter.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class DigitalStampDTO {

    protected String type;
    protected byte[] documentHash;
    protected String province;
}
