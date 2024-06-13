package it.gov.pagopa.wispconverter.service.model.session;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionDataDTO {

    private CommonFieldsDTO commonFields;
    private Map<String, PaymentNoticeContentDTO> paymentNotices;
    private Map<String, RPTContentDTO> rpts;

    public PaymentNoticeContentDTO getPaymentNoticeByIUV(String iuv) {
        return this.paymentNotices.get(iuv);
    }

    public PaymentNoticeContentDTO getPaymentNoticeByNoticeNumber(String noticeNumber) {
        return this.paymentNotices.values().stream()
                .filter(paymentNotice -> noticeNumber.equals(paymentNotice.getNoticeNumber()))
                .findFirst()
                .orElse(null);
    }

    public Collection<PaymentNoticeContentDTO> getAllPaymentNotices() {
        return this.paymentNotices.values();
    }

    public void addPaymentNotice(PaymentNoticeContentDTO paymentNotice) {
        this.paymentNotices.put(paymentNotice.getIuv(), paymentNotice);
    }

    public int getNumberOfRPT() {
        return this.rpts.values().size();
    }

    public RPTContentDTO getFirstRPT() {
        return this.rpts.values().stream()
                .filter(rpt -> rpt.getIndex() == 1)
                .findFirst()
                .orElse(null);
    }

    public Collection<RPTContentDTO> getAllRPTs() {
        return this.rpts.values();
    }

    public List<String> getNAVs() {
        return this.paymentNotices.values().stream()
                .map(PaymentNoticeContentDTO::getNoticeNumber)
                .toList();
    }

    public RPTContentDTO getRPTByIUV(String iuv) {
        return this.rpts.get(iuv);
    }

}
