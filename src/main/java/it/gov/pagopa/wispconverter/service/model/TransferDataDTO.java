package it.gov.pagopa.wispconverter.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferDataDTO {

    protected XMLGregorianCalendar paymentDate;
    protected BigDecimal totalAmount;
    protected String type;
    protected String iuv;
    protected String ccp;
    protected String debitIban;
    protected String debitBic;
    protected String rtSignature;
    protected List<TransferDTO> transfer;
}
