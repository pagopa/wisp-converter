package it.gov.pagopa.wispconverter.service.model.session;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonFieldsDTO {

    private String sessionId;
    private String cartId;
    private String creditorInstitutionId;
    private String creditorInstitutionName;
    private String pspId;
    private String creditorInstitutionBrokerId;
    private String stationId;
    private String channelId;
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
    private String signatureType;
}