package it.gov.pagopa.wispconverter.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConversionResultDTO {
    private boolean success;
    private String errorPage;
    private String uri;
}
