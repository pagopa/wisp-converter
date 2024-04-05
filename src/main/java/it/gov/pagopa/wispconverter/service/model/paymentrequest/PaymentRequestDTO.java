package it.gov.pagopa.wispconverter.service.model.paymentrequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.gov.pagopa.wispconverter.service.model.PaymentRequestDomainDTO;
import it.gov.pagopa.wispconverter.service.model.PaymentSubjectDTO;
import it.gov.pagopa.wispconverter.service.model.TransferDataDTO;
import lombok.*;

import javax.xml.datatype.XMLGregorianCalendar;


@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentRequestDTO {

    private String version;
    private PaymentRequestDomainDTO domain;
    private String messageRequestId;
    private XMLGregorianCalendar messageRequestDatetime;
    private SubjectAuthentication subjectAuthentication;
    private PaymentSubjectDTO debtor; // versante
    private PaymentSubjectDTO payer; // pagatore
    private PaymentSubjectDTO payeeInstitution;
    private TransferDataDTO transferData;
}
