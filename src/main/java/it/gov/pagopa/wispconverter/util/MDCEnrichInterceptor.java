package it.gov.pagopa.wispconverter.util;

import it.gov.pagopa.wispconverter.controller.advice.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Slf4j
public class MDCEnrichInterceptor implements HandlerInterceptor {

//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        log.debug("[preHandle][" + request + "]");
//        return true;
//    }
//
//    @Override
//    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
//        log.debug("[postHandle][" + request + "]");
//    }

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
