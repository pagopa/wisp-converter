package it.gov.pagopa.wispconverter.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class MDCEnrichInterceptor implements HandlerInterceptor {
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if(!MDCUtil.hasStatus()){
            log.debug("[afterCompletion] configure status");
            MDCUtil.setMDCCloseSuccessOperation(response.getStatus());
        } else {
            log.debug("[afterCompletion] status already configured");
        }
    }

}
