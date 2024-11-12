package it.gov.pagopa.wispconverter.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.util.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static it.gov.pagopa.wispconverter.util.CommonUtility.deNull;

@Aspect
@Component
@Slf4j
public class LoggingAspect {


  final HttpServletRequest httRequest;

  final HttpServletResponse httpResponse;

  @Value("${info.application.name}")
  private String name;

  @Value("${info.application.version}")
  private String version;

  @Value("${info.properties.environment}")
  private String environment;

  public LoggingAspect(HttpServletRequest httRequest, HttpServletResponse httpResponse) {
    this.httRequest = httRequest;
    this.httpResponse = httpResponse;
  }

  private static String getDetail(ResponseEntity<Object> result) {
    if (result != null && result.getBody() != null && result.getBody() instanceof ProblemDetail problemDetail
    		&& problemDetail.getDetail() != null) {
      return problemDetail.getDetail();
    } else return AppErrorCodeMessageEnum.UNKNOWN.getDetail();
  }

  private static String getTitle(ResponseEntity<Object> result) {
    if (result != null && result.getBody() != null && result.getBody() instanceof ProblemDetail problemDetail
    		&& problemDetail.getTitle() != null) {
      return problemDetail.getTitle();
    } else return AppErrorCodeMessageEnum.UNKNOWN.getTitle();
  }

  public static String getExecutionTime() {
    String startTime = MDC.get(Constants.MDC_EXECUTION_TIME);
    if (startTime != null) {
      long endTime = System.currentTimeMillis();
      long executionTime = endTime - Long.parseLong(startTime);
      return String.valueOf(executionTime);
    }
    return "-";
  }

  private static Map<String, String> getParams(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    Map<String, String> params = new HashMap<>();
    int i = 0;
    for (var parameter : method.getParameters()) {
      var paramName = parameter.getName();
      var arg = joinPoint.getArgs()[i++];
      if (arg instanceof JAXBElement<?>) {
          try {
              arg = new ObjectMapper().writer().writeValueAsString(arg);
          } catch (JsonProcessingException e) {
              arg = "unreadable!";
          }
      }
      params.put(paramName, deNull(arg));
    }
    return params;
  }

  @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
  public void restController() {
    // all rest controllers
  }

  @Pointcut("@within(org.springframework.stereotype.Repository)")
  public void repository() {
    // all repository methods
  }

  @Pointcut("@within(org.springframework.stereotype.Service)")
  public void service() {
    // all service methods
  }

  /** Log essential info of application during the startup. */
  @PostConstruct
  public void logStartup() {
    log.info("-> Starting {} version {} - environment {}", name, version, environment);
  }

  @Around(value = "restController()")
  public Object logApiInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
    Map<String, String> params = getParams(joinPoint);
    MDC.put(Constants.MDC_ARGS, params.toString());

    log.debug("Invoking API operation{} - args: {}", joinPoint.getSignature().getName(), params);

    Object result = joinPoint.proceed();

    log.info("Successful API operation {} - result: {}", joinPoint.getSignature().getName(), result);
    return result;
  }


  @Around(value = "repository() || service()")
  public Object logTrace(ProceedingJoinPoint joinPoint) throws Throwable {
    Map<String, String> params = getParams(joinPoint);
    log.debug("Call method {} - args: {}", joinPoint.getSignature().toShortString(), params);
    Object result = joinPoint.proceed();
    log.debug("Return method {} - result: {}", joinPoint.getSignature().toShortString(), result);
    return result;
  }
}
