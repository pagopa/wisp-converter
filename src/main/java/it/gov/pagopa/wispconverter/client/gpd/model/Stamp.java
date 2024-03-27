package it.gov.pagopa.wispconverter.client.gpd.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stamp implements Serializable {

    @NotBlank
    @Schema(required = true, description = "Document hash")
    private String hashDocument;

    @NotBlank
    @Size(min = 2, max = 2)
    @Schema(required = true, description = "The type of the stamp", minLength = 2, maxLength = 2)
    private String stampType;

    @NotBlank
    @Pattern(regexp = "[A-Z]{2}")
    @Schema(required = true, description = "The provincial of the residence", example = "RM", pattern = "[A-Z]{2,2}")
    private String provincialResidence;
}
