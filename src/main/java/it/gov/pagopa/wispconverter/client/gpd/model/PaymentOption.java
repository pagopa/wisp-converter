package it.gov.pagopa.wispconverter.client.gpd.model;

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

    private String iuv;

    private Long amount;

    private String description;

    private Boolean isPartialPayment;

    private LocalDateTime dueDate;

    private LocalDateTime retentionDate;

    private long fee;

    private long notificationFee;

    private List<Transfer> transfer = new ArrayList<>();

    private List<PaymentOptionMetadata> paymentOptionMetadata = new ArrayList<>();
}
