package it.gov.pagopa.wispconverter.util;

import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;

import java.util.Map;

public class MDCUtil {

    private static final String FAILED = "Failed";
    private static final String SUCCESS = "Success";
    public static void setMDCError(ProblemDetail problemDetail){
        MDC.put(Constants.MDC_ERROR_TITLE, problemDetail.getTitle());
        MDC.put(Constants.MDC_ERROR_DETAIL, problemDetail.getDetail());

        Map<String, Object> properties = problemDetail.getProperties();
        if (properties != null) {
            String errorCode = (String)properties.get(ErrorUtil.EXTRA_FIELD_ERROR_CODE);
            MDC.put(Constants.MDC_ERROR_CODE, errorCode);
        }
    }
    public static void setMDCCloseSuccessOperation(int statusCode){
        setMDCCloseOperation(SUCCESS, statusCode);
    }
    public static void setMDCCloseFailedOperation(int statusCode){
        setMDCCloseOperation(FAILED, statusCode);
    }
     private static void setMDCCloseOperation(String status, int statusCode){
        MDC.put(Constants.MDC_STATUS, status);
        MDC.put(Constants.MDC_STATUS_CODE, String.valueOf(statusCode));
        String executionTime = CommonUtility.getExecutionTime(MDC.get(Constants.MDC_START_TIME));
        MDC.put(Constants.MDC_EXECUTION_TIME, executionTime);
    }

    public static boolean hasStatus(){
        return MDC.get(Constants.MDC_STATUS) != null;
    }
}
