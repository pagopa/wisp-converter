package it.gov.pagopa.wispconverter.client.gpd.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferMetadata implements Serializable {

    @NotBlank(message = "key is required")
    private String key;

    private String value;

}
