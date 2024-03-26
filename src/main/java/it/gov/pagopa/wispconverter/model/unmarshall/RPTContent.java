package it.gov.pagopa.wispconverter.model.unmarshall;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.gov.digitpa.schemas._2011.pagamenti.CtRichiestaPagamentoTelematico;
import lombok.*;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class RPTContent<T> {
    private T wrappedRPT;
    private CtRichiestaPagamentoTelematico rpt;
}
