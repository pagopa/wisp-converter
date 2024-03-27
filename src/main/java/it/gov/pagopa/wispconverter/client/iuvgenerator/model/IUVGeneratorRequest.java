package it.gov.pagopa.wispconverter.client.iuvgenerator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.io.Serializable;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class IUVGeneratorRequest implements Serializable {
    private String segregationCode;
    private String auxDigit;
}
