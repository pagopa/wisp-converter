package it.gov.pagopa.wispconverter.util.interceptor;

import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.EndpointRETrace;
import it.gov.pagopa.wispconverter.util.ReUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@RequiredArgsConstructor
public class ReInterceptor implements HandlerInterceptor {

    private final ReService reService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (handler instanceof HandlerMethod handlerMethod) {
            EndpointRETrace trace = handlerMethod.getMethod().getAnnotation(EndpointRETrace.class);
            if (trace != null && trace.reEnabled()) {
                String businessProcess = trace.businessProcess();
                log.debug("[afterCompletion] trace RE SERVER OUT businessProcess = [{}]", businessProcess);
                MDC.put(Constants.MDC_STATUS, trace.status().name());
                MDC.put(Constants.MDC_CLIENT_EXECUTION_TIME, CommonUtility.getExecutionTime(CommonUtility.getExecutionTime(MDC.get(Constants.MDC_START_TIME))));
                reService.addRe(ReUtil.createEventForEndpoint(request, response));
            }
        }
    }
}
