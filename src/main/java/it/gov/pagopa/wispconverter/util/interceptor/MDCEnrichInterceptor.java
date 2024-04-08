package it.gov.pagopa.wispconverter.util.interceptor;

import it.gov.pagopa.wispconverter.util.MDCUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class MDCEnrichInterceptor implements HandlerInterceptor {

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if(handler instanceof HandlerMethod) {
            if (!MDCUtil.hasStatus()) {
                log.debug("[afterCompletion] configure status");
                MDCUtil.setMDCCloseSuccessOperation(response.getStatus());
            } else {
                log.debug("[afterCompletion] status already configured");
            }
        }
    }

}
