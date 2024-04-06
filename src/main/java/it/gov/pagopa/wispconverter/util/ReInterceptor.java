package it.gov.pagopa.wispconverter.util;

import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@RequiredArgsConstructor
public class ReInterceptor implements HandlerInterceptor {

    private final ReService reService;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.debug("[preHandle] add RE SERVER IN");
        ReEventDto reEventDtoServerIN = ReUtil.createReServerInterfaceRequest(request);
        reService.addRe(reEventDtoServerIN);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        log.debug("[afterCompletion] add RE SERVER OUT");
        ReEventDto reEventDtoServerOUT = ReUtil.createReServerInterfaceResponse(request, response);
        reService.addRe(reEventDtoServerOUT);
    }

}
