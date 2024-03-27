package it.gov.pagopa.wispconverter.client.gpd.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class PaymentOptionMetadata implements Serializable {

    @NotBlank(message = "key is required")
    private String key;

    private String value;
}
