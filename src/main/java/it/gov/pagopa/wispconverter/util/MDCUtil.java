package it.gov.pagopa.wispconverter.util;

import it.gov.pagopa.wispconverter.service.model.session.CommonFieldsDTO;
import it.gov.pagopa.wispconverter.service.model.session.RPTContentDTO;
import it.gov.pagopa.wispconverter.service.model.session.SessionDataDTO;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;

import java.util.Map;

public class MDCUtil {

    private static final String FAILED = "Failed";
    private static final String SUCCESS = "Success";

    public static void setSessionDataInfoInMDC(SessionDataDTO sessionData, String primitive) {

        if ("TRUE".equals(MDC.get(Constants.MDC_CONTROL_FLAG))) {

            CommonFieldsDTO commonFields = sessionData.getCommonFields();
            MDC.put(Constants.MDC_CONTROL_FLAG, "TRUE");

            MDC.put(Constants.MDC_CART_ID, commonFields.getCartId());
            MDC.put(Constants.MDC_DOMAIN_ID, commonFields.getCreditorInstitutionId());
            MDC.put(Constants.MDC_STATION_ID, commonFields.getStationId());
            MDC.put(Constants.MDC_CHANNEL_ID, commonFields.getChannelId());
            MDC.put(Constants.MDC_PSP_ID, commonFields.getPspId());

            if (primitive != null) {

                MDC.put(Constants.MDC_PRIMITIVE, primitive);

                // if the primitive is nodoInviaCarrelloRPT, it means that a cart was extracted, so set cartId in MDC. Otherwise, set IUV and CCP in MDC
                if (Constants.NODO_INVIA_CARRELLO_RPT.equals(primitive)) {
                    MDC.put(Constants.MDC_CART_ID, commonFields.getCartId());
                } else {
                    RPTContentDTO singleRpt = sessionData.getFirstRPT();
                    MDC.put(Constants.MDC_IUV, singleRpt.getIuv());
                    MDC.put(Constants.MDC_CCP, singleRpt.getCcp());
                }
            }
        }
    }

    public static void setMDCError(ProblemDetail problemDetail) {
        MDC.put(Constants.MDC_ERROR_TITLE, problemDetail.getTitle());
        MDC.put(Constants.MDC_ERROR_DETAIL, problemDetail.getDetail());

        Map<String, Object> properties = problemDetail.getProperties();
        if (properties != null) {
            String errorCode = (String) properties.get(ErrorUtil.EXTRA_FIELD_ERROR_CODE);
            MDC.put(Constants.MDC_ERROR_CODE, errorCode);
        }
    }

    public static void setMDCCloseSuccessOperation(int statusCode) {
        setMDCCloseOperation(SUCCESS, statusCode);
    }

    public static void setMDCCloseFailedOperation(int statusCode) {
        setMDCCloseOperation(FAILED, statusCode);
    }

    private static void setMDCCloseOperation(String status, int statusCode) {
        MDC.put(Constants.MDC_STATUS, status);
        MDC.put(Constants.MDC_STATUS_CODE, String.valueOf(statusCode));
        String executionTime = CommonUtility.getExecutionTime(MDC.get(Constants.MDC_START_TIME));
        MDC.put(Constants.MDC_EXECUTION_TIME, executionTime);
    }

    public static boolean hasStatus() {
        return MDC.get(Constants.MDC_STATUS) != null;
    }
}
