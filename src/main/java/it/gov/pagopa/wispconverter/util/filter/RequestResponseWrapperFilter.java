package it.gov.pagopa.wispconverter.util.filter;

import it.gov.pagopa.wispconverter.util.Constants;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.List;

@Slf4j
@Getter
@Setter
public class RequestResponseWrapperFilter extends OncePerRequestFilter {

  private List<String> excludeUrlPatterns;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    boolean isFirstRequest = !isAsyncDispatch(request);
    HttpServletRequest requestToUse = request;
    HttpServletResponse responseToUse = response;

    if (isFirstRequest && !(request instanceof RepeatableContentCachingRequestWrapper)) {
      requestToUse = new RepeatableContentCachingRequestWrapper(request);
    }

    if (!(response instanceof ContentCachingResponseWrapper)) {
      responseToUse = new ContentCachingResponseWrapper(response);
    }
    try {
      String requestId = MDC.get(Constants.MDC_REQUEST_ID);
      log.debug("RequestResponseWrapperFilter - wrap req and resp for {}=[{}]", Constants.MDC_REQUEST_ID, requestId != null ? requestId : "na");
      filterChain.doFilter(requestToUse, responseToUse);
    } finally {
      if (request.isAsyncStarted()) {
        HttpServletResponse finalResponseToUse = responseToUse;
        request
            .getAsyncContext()
            .addListener(
                new AsyncListener() {
                  public void onComplete(AsyncEvent asyncEvent) throws IOException {
                    ((ContentCachingResponseWrapper) finalResponseToUse).copyBodyToResponse();
                  }

                  public void onTimeout(AsyncEvent asyncEvent) { /* empty */ }

                  public void onError(AsyncEvent asyncEvent) { /* empty */ }

                  public void onStartAsync(AsyncEvent asyncEvent) { /* empty */ }
                });
      } else {
        ((ContentCachingResponseWrapper) responseToUse).copyBodyToResponse();
      }
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    AntPathMatcher pathMatcher = new AntPathMatcher();
    return excludeUrlPatterns
            .stream()
            .anyMatch(p -> pathMatcher.match(p, request.getServletPath()));
  }
}
