package it.gov.pagopa.wispconverter.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.gov.digitpa.schemas._2011.pagamenti.CtRichiestaPagamentoTelematico;
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
    private String noticeNumber;
    // TODO disaccoppia CtRichiestaPagamentoTelematico, estraendo TUTTI i campi
    private CtRichiestaPagamentoTelematico rpt;
}
