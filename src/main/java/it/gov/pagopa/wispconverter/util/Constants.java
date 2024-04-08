package it.gov.pagopa.wispconverter.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    public static final String SERVICE_CODE_APP = "WIC";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    public static final String PA_INVIA_RT = "paaInviaRT";
    public static final String SEND_PAYMENT_RESULT_V2 = "sendPaymentResultV2";
    public static final String NODO_INVIA_RPT = "nodoInviaRPT";
    public static final String NODO_INVIA_CARRELLO_RPT = "nodoInviaCarrelloRPT";
    public static final String CLOSE_PAYMENT_V2 = "closePaymentV2";

    public static final String MDC_START_TIME = "startTime";
    public static final String MDC_STATUS = "status";
    public static final String MDC_STATUS_CODE = "httpCode";
    public static final String MDC_EXECUTION_TIME = "executionTime";
    public static final String MDC_ERROR_CODE = "errorCode";
    public static final String MDC_ERROR_TITLE = "errorTitle";
    public static final String MDC_ERROR_DETAIL = "errorDetail";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_OPERATION_ID = "operationId";

    public static final String MDC_BUSINESS_PROCESS = "businessProcess";

    public static final String MDC_CLIENT_OPERATION_ID = "clientOperationId";
    public static final String MDC_CLIENT_EXECUTION_TIME = "clientExecutionTime";

    public static final String MDC_EROGATORE = "erogatore";
    public static final String MDC_EROGATORE_DESCR = "erogatoreDescr";



}
