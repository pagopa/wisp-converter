//package it.gov.pagopa.wispconverter.util.filter;
//
//import it.gov.pagopa.wispconverter.util.client.ServerLoggingProperties;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//public class AppServerLoggingFilterFilter extends AbstractAppServerLoggingFilter {
//
//  public AppServerLoggingFilterFilter(ServerLoggingProperties serverLoggingProperties){
//    super(serverLoggingProperties);
//  }
//
//  @Override
//  protected void request(String operationId, HttpServletRequest request) {
//    if(log.isDebugEnabled()){
//      log.debug(createRequestMessage(operationId, request));
//    }
//  }
//
//  @Override
//  protected void response(String operationId, String executionTime, HttpServletRequest request, HttpServletResponse response) {
//    if(log.isDebugEnabled()){
//      log.debug(createResponseMessage(operationId, executionTime, request, response));
//    }
//  }
//}
