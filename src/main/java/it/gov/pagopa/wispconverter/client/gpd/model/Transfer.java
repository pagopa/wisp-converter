package it.gov.pagopa.wispconverter.client.gpd.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Transfer implements Serializable {

    @NotBlank(message = "id transfer is required")
    @Schema(type = "string", allowableValues = {"1", "2", "3", "4", "5"})
    private String idTransfer;

    @NotNull(message = "amount is required")
    private Long amount;

    @Schema(description = "Fiscal code related to the organization targeted by this transfer.", example = "00000000000")
    private String organizationFiscalCode;

    @NotBlank(message = "remittance information is required")
    private String remittanceInformation; // causale

    @NotBlank(message = "category is required")
    private String category; // taxonomy

    @Schema(description = "mutual exclusive with stamp", example = "IT0000000000000000000000000")
    private String iban;

    @Schema(description = "optional - can be combined with iban but not with stamp", example = "IT0000000000000000000000000")
    private String postalIban;

    @Schema(description = "mutual exclusive with iban and postalIban")
    private Stamp stamp;

    @Valid
    @Size(min = 0, max = 10)
    @Schema(description = "it can added a maximum of 10 key-value pairs for metadata")
    private List<TransferMetadata> transferMetadata = new ArrayList<>();

    public void addTransferMetadata(TransferMetadata trans) {
        transferMetadata.add(trans);
    }

    public void removeTransferMetadata(TransferMetadata trans) {
        transferMetadata.remove(trans);
    }

}
