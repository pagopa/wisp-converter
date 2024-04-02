package it.gov.pagopa.wispconverter.util.filter;

import it.gov.pagopa.wispconverter.util.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

import static it.gov.pagopa.wispconverter.util.Constants.HEADER_REQUEST_ID;



@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // get requestId from header or generate one
        String requestId = request.getHeader(HEADER_REQUEST_ID);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
            log.debug("RequestIdFilter - assign new {}=[{}]", HEADER_REQUEST_ID, requestId);
        } else{
            log.debug("RequestIdFilter - found {}=[{}]", HEADER_REQUEST_ID, requestId);
        }

        // set requestId in MDC
        MDC.put(Constants.MDC_REQUEST_ID, requestId);

        // set requestId in the response header
        ((HttpServletResponse) response).setHeader(HEADER_REQUEST_ID, requestId);

        filterChain.doFilter(request, response);
    }

}
