//package it.gov.pagopa.wispconverter.util.filter;
//
//import it.gov.pagopa.wispconverter.service.ReService;
//import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
//import it.gov.pagopa.wispconverter.util.ReUtil;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.Setter;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.util.AntPathMatcher;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.List;
//
//
//@Slf4j
//@RequiredArgsConstructor
//public class ReFilter extends OncePerRequestFilter {
//
//    private final ReService reService;
//
//    @Setter
//    private List<String> excludeUrlPatterns;
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        ReEventDto reEventDtoServerIN = ReUtil.createReServerInterfaceRequest(request);
//        try{
////            MDC.getCopyOfContextMap().forEach((k,v) -> {
////                log.debug(String.format("SERVER BEFORE MDC %s=%s",k, v));
////            });
//            reService.addRe(reEventDtoServerIN);
//            filterChain.doFilter(request, response);
//        } finally {
////            MDC.getCopyOfContextMap().forEach((k,v) -> {
////                log.debug(String.format("SERVER AFTER MDC %s=%s",k, v));
////            });
//            ReEventDto reEventDtoServerOUT = ReUtil.createReServerInterfaceResponse(request, response);
//            reService.addRe(reEventDtoServerOUT);
//        }
//
//    }
//
//    @Override
//    protected boolean shouldNotFilter(HttpServletRequest request) {
//        AntPathMatcher pathMatcher = new AntPathMatcher();
//        return excludeUrlPatterns
//                .stream()
//                .anyMatch(p -> pathMatcher.match(p, request.getServletPath()));
//    }
//
//}
