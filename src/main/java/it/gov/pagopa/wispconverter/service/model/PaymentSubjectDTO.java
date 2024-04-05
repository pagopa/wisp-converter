package it.gov.pagopa.wispconverter.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentSubjectDTO {

    protected SubjectUniqueIdentifierDTO subjectUniqueIdentifier;

    protected String name;

    protected String operUnitCode;

    protected String operUnitDenom;

    protected String address;

    protected String streetNumber;

    protected String postalCode;

    protected String city;

    protected String province;

    protected String nation;

    protected String email;
}
