package it.gov.pagopa.wispconverter.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonRPTFieldsDTO {

    private String cartId;
    private String iupd;
    private String iuv;
    private String nav;
    private String creditorInstitutionId;
    private String creditorInstitutionBrokerId;
    private String stationId;
    private String payerType;
    private String payerFiscalCode;
    private String payerFullName;
    private String payerAddressStreetName;
    private String payerAddressStreetNumber;
    private String payerAddressPostalCode;
    private String payerAddressCity;
    private String payerAddressProvince;
    private String payerAddressNation;
    private String payerEmail;
    private Boolean isMultibeneficiary;
    private Boolean containsDigitalStamp;
    private List<RPTContentDTO> rpts;
}
