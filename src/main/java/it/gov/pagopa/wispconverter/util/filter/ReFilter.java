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
import java.util.Map;
import java.util.UUID;

import static it.gov.pagopa.wispconverter.util.Constants.HEADER_REQUEST_ID;


@Slf4j
public class ReFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try{
            MDC.getCopyOfContextMap().forEach((k,v) -> {
                log.info(String.format("BEFORE MDC %s=%s",k, v));
            });
            filterChain.doFilter(request, response);
        } finally {
            MDC.getCopyOfContextMap().forEach((k,v) -> {
                log.info(String.format("AFTER MDC %s=%s",k, v));
            });
        }
    }

}
