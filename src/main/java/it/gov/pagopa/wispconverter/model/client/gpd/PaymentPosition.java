package it.gov.pagopa.wispconverter.model.client.gpd;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class PaymentPosition implements Serializable {

    @NotBlank(message = "iupd is required")
    private String iupd;

    @NotNull(message = "type is required")
    private Type type;

    @NotBlank(message = "fiscal code is required")
    private String fiscalCode;

    @NotBlank(message = "full name is required")
    private String fullName;

    private String streetName;

    private String civicNumber;

    private String postalCode;

    private String city;

    private String province;

    private String region;

    @Pattern(regexp = "[A-Z]{2}", message = "The country must be reported with two capital letters (example: IT)")
    private String country;

    @Email(message = "Please provide a valid email address")
    private String email;

    private String phone;

    @NotNull(message = "switch to expired value is required")
    private Boolean switchToExpired;

    // Payment Position properties
    @NotBlank(message = "company name is required")
    private String companyName;

    private String officeName;

    private LocalDateTime validityDate;

    @JsonProperty(access = Access.READ_ONLY)
    private LocalDateTime paymentDate;

    @JsonProperty(access = Access.READ_ONLY)
    private DebtPositionStatus status;

    @Valid
    private List<@Valid PaymentOption> paymentOption = new ArrayList<>();

    public void addPaymentOptions(PaymentOption paymentOpt) {
        paymentOption.add(paymentOpt);
    }

    public void removePaymentOptions(PaymentOption paymentOpt) {
        paymentOption.remove(paymentOpt);
    }
}
