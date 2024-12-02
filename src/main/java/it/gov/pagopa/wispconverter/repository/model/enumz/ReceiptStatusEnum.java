package it.gov.pagopa.wispconverter.repository.model.enumz;

public enum ReceiptStatusEnum {
    REDIRECT,
    // PAYING status track the execution flow between
    // (DELETE receipt/timer, POST receipt/ko call), -> e.g. ClosePayment inbound and (ClosePayment KO or SPR KO)
    // (DELETE receipt/timer, POST receipt/ok call) -> e.g. ClosePayment inbound and paSendRTV2
    PAYING,
    SENDING,
    SCHEDULED,
    SENT,
    NOT_SENT,
    SENT_REJECTED_BY_EC
}