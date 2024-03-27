package it.gov.pagopa.wispconverter.client.gpd.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class PaymentPosition implements Serializable {

    private String iupd;

    private Type type;

    private String fiscalCode;

    private String fullName;

    private String streetName;

    private String civicNumber;

    private String postalCode;

    private String city;

    private String province;

    private String region;

    private String country;

    private String email;

    private String phone;

    private Boolean switchToExpired;

    private String companyName;

    private String officeName;

    private LocalDateTime validityDate;

    private LocalDateTime paymentDate;

    private DebtPositionStatus status;

    private List<PaymentOption> paymentOption = new ArrayList<>();
}
