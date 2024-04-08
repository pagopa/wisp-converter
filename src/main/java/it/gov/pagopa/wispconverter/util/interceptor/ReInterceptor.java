package it.gov.pagopa.wispconverter.util.interceptor;

import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ReUtil;
import it.gov.pagopa.wispconverter.util.TraceReEvent;
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
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(handler instanceof HandlerMethod){
            TraceReEvent traceReEvent = ((HandlerMethod) handler).getMethod().getAnnotation(TraceReEvent.class);
            if(traceReEvent!=null){
                String businessProcess = traceReEvent.businessProcess();
                MDC.put(Constants.MDC_BUSINESS_PROCESS, businessProcess);
                log.debug("[preHandle] trace RE SERVER IN businessProcess = [{}]", businessProcess);
                ReEventDto reEventDtoServerIN = ReUtil.createReServerInterfaceRequest(request);
                reService.addRe(reEventDtoServerIN);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if(handler instanceof HandlerMethod){
            TraceReEvent traceReEvent = ((HandlerMethod) handler).getMethod().getAnnotation(TraceReEvent.class);
            if(traceReEvent!=null){
                String businessProcess = traceReEvent.businessProcess();
                log.debug("[afterCompletion] trace RE SERVER OUT businessProcess = [{}]", businessProcess);
                ReEventDto reEventDtoServerOUT = ReUtil.createReServerInterfaceResponse(request, response);
                reService.addRe(reEventDtoServerOUT);
            }
        }
    }


}
