package it.gov.pagopa.wispconverter.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.gov.pagopa.wispconverter.service.model.paymentrequest.PaymentRequestDTO;
import lombok.*;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class RPTContentDTO {
    private String idDominio;
    private String idIntermediarioPA;
    private Boolean multibeneficiario;
    private String nav;
    private PaymentRequestDTO rpt;
    //private CtRichiestaPagamentoTelematico rpt;


}
