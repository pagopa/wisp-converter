package it.gov.pagopa.wispconverter.model.client.gpd;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class PaymentOption implements Serializable {

    private String nav;

    @NotBlank(message = "iuv is required")
    private String iuv;

    @NotNull(message = "amount is required")
    private Long amount;

    private String description;

    @NotNull(message = "is partial payment is required")
    private Boolean isPartialPayment;

    @NotNull(message = "due date is required")
    private LocalDateTime dueDate;

    private LocalDateTime retentionDate;

    private long fee;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private long notificationFee;

    @Valid
    private List<Transfer> transfer = new ArrayList<>();

    @Valid
    @Size(min = 0, max = 10)
    @Schema(description = "it can added a maximum of 10 key-value pairs for metadata")
    private List<PaymentOptionMetadata> paymentOptionMetadata = new ArrayList<>();

    public void addTransfers(Transfer trans) {
        transfer.add(trans);
    }

    public void removeTransfers(Transfer trans) {
        transfer.remove(trans);
    }

    public void addPaymentOptionMetadata(PaymentOptionMetadata paymentOptMetadata) {
        paymentOptionMetadata.add(paymentOptMetadata);
    }

    public void removePaymentOptionMetadata(PaymentOptionMetadata paymentOptMetadata) {
        paymentOptionMetadata.remove(paymentOptMetadata);
    }
}
