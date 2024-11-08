package it.gov.pagopa.wispconverter.service.model.re;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RePaymentContext {

    private String cart;
    private String iuv;
    private String noticeNumber;
    private String ccp;
    private String paymentToken;
    private String domainId;
    private String pspId;
    private String stationId;
    private String channelId;
}
