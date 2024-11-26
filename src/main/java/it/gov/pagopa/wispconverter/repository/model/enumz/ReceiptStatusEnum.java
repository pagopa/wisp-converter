package it.gov.pagopa.wispconverter.repository.model.enumz;

public enum ReceiptStatusEnum {
    REDIRECT,
    PAYING, // the execution flow between ClosePayment sending and paSendRTV2
    SENDING,
    SCHEDULED,
    SENT,
    NOT_SENT,
    SENT_REJECTED_BY_EC
}