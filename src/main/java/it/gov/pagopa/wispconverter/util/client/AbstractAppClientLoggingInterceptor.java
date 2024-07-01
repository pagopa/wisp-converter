package it.gov.pagopa.wispconverter.util.client;

import it.gov.pagopa.wispconverter.repository.model.enumz.*;
import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ReUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class AbstractAppClientLoggingInterceptor implements ClientHttpRequestInterceptor {

    public static final String REQUEST_DEFAULT_MESSAGE_PREFIX = "===> CLIENT Request OPERATION_ID=%s, CLIENT_OPERATION_ID=%s - ";
    public static final String RESPONSE_DEFAULT_MESSAGE_PREFIX = "<=== CLIENT Response OPERATION_ID=%s, CLIENT_OPERATION_ID=%s -";
    private static final int REQUEST_DEFAULT_MAX_PAYLOAD_LENGTH = 50;
    private static final int RESPONSE_DEFAULT_MAX_PAYLOAD_LENGTH = 50;
    private static final String SPACE = " ";
    private static final String PRETTY_OUT = "\n===> *";
    private static final String PRETTY_IN = "\n<=== *";
    private final ReService reService;
    protected ClientServiceEnum clientServiceEnum;
    private boolean requestIncludeHeaders;
    private boolean responseIncludeHeaders;
    private boolean requestIncludePayload;
    private boolean responseIncludePayload;
    private Predicate<String> requestHeaderPredicate;
    private Predicate<String> responseHeaderPredicate;
    private int requestMaxPayloadLength = REQUEST_DEFAULT_MAX_PAYLOAD_LENGTH;
    private int responseMaxPayloadLength = RESPONSE_DEFAULT_MAX_PAYLOAD_LENGTH;
    private boolean requestPretty;
    private boolean responsePretty;
    private boolean mustPersistEventOnRE;

    protected AbstractAppClientLoggingInterceptor(RequestResponseLoggingProperties clientLoggingProperties, ReService reService, ClientServiceEnum clientServiceEnum) {
        this.reService = reService;
        this.clientServiceEnum = clientServiceEnum;
        this.mustPersistEventOnRE = true;

        if (clientLoggingProperties != null) {
            RequestResponseLoggingProperties.Request request = clientLoggingProperties.getRequest();
            if (request != null) {
                this.requestIncludeHeaders = request.isIncludeHeaders();
                this.requestIncludePayload = request.isIncludePayload();
                this.requestMaxPayloadLength = request.getMaxPayloadLength() != null ? request.getMaxPayloadLength() : REQUEST_DEFAULT_MAX_PAYLOAD_LENGTH;
                this.requestHeaderPredicate = s -> !s.equals(request.getMaskHeaderName());
                this.requestPretty = request.isPretty();
            }
            RequestResponseLoggingProperties.Response response = clientLoggingProperties.getResponse();
            if (response != null) {
                this.responseIncludeHeaders = response.isIncludeHeaders();
                this.responseIncludePayload = response.isIncludePayload();
                this.responseMaxPayloadLength = response.getMaxPayloadLength() != null ? response.getMaxPayloadLength() : RESPONSE_DEFAULT_MAX_PAYLOAD_LENGTH;
                this.responseHeaderPredicate = null;
                this.responsePretty = response.isPretty();
            }
        }

    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        String startClient = String.valueOf(System.currentTimeMillis());
        String clientOperationId = UUID.randomUUID().toString();
        MDC.put(Constants.MDC_CLIENT_OPERATION_ID, clientOperationId);
        MDC.put(Constants.MDC_CLIENT_SERVICE_ID, clientServiceEnum.label);
        String operationId = MDC.get(Constants.MDC_OPERATION_ID);
        request(clientOperationId, operationId, request, body);
        ClientEnum clientType = ClientEnum.valueOf(MDC.get(Constants.MDC_CLIENT_TYPE));

        ClientHttpResponse response;
        try {
            response = execution.execute(request, body);

            String executionClientTime = CommonUtility.getExecutionTime(startClient);
            MDC.put(Constants.MDC_CLIENT_EXECUTION_TIME, executionClientTime);

            MDC.put(Constants.MDC_STATUS_CODE, String.valueOf(response.getStatusCode().value()));
            MDC.put(Constants.MDC_CALL_TYPE, CallTypeEnum.CLIENT.name());
            MDC.put(Constants.MDC_EVENT_CATEGORY, EventCategoryEnum.INTERFACE.name());

            MDC.put(Constants.MDC_EVENT_SUB_CATEGORY, EventSubcategoryEnum.REQ.name());
            log.debug("[intercept] add RE CLIENT OUT - Sent");
            ReEventDto reEventDtoClientIN = ReUtil.createREForClientInterfaceInRequestEvent(request, body, clientType, OutcomeEnum.SEND);
            persistInterfaceEventInRE(reEventDtoClientIN);

            if (response.getStatusCode().is2xxSuccessful()) {
                MDC.put(Constants.MDC_EVENT_SUB_CATEGORY, EventSubcategoryEnum.RESP.name());
                log.debug("[intercept] add RE CLIENT IN - Sent - RECEIVED");
                ReEventDto reEventDtoClientOUT = ReUtil.createREForClientInterfaceInResponseEvent(request, response, clientType, OutcomeEnum.RECEIVED);
                persistInterfaceEventInRE(reEventDtoClientOUT);
            } else {
                MDC.put(Constants.MDC_EVENT_SUB_CATEGORY, EventSubcategoryEnum.RESP.name());
                log.debug("[intercept] add RE CLIENT IN - Sent - RECEIVED_FAILURE");
                ReEventDto reEventDtoClientOUT = ReUtil.createREForClientInterfaceInResponseEvent(request, response, clientType, OutcomeEnum.RECEIVED_FAILURE);
                persistInterfaceEventInRE(reEventDtoClientOUT);
            }


            response(clientOperationId, operationId, executionClientTime, request, response);

        } catch (Exception e) {
            String executionClientTime = CommonUtility.getExecutionTime(startClient);
            MDC.put(Constants.MDC_CLIENT_EXECUTION_TIME, executionClientTime);

            MDC.put(Constants.MDC_EVENT_SUB_CATEGORY, EventSubcategoryEnum.REQ.name());
            log.debug("[intercept] add RE CLIENT OUT - NOT Sent");
            ReEventDto reEventDtoClientIN = ReUtil.createREForClientInterfaceInRequestEvent(request, body, clientType, OutcomeEnum.SEND_FAILURE);
            persistInterfaceEventInRE(reEventDtoClientIN);

            MDC.put(Constants.MDC_EVENT_SUB_CATEGORY, EventSubcategoryEnum.RESP.name());
            log.debug("[intercept] add RE CLIENT IN - NOT Sent - NEVER_RECEIVED");
            ReEventDto reEventDtoClientOUT = ReUtil.createREForClientInterfaceInResponseEvent(request, null, clientType, OutcomeEnum.NEVER_RECEIVED);
            persistInterfaceEventInRE(reEventDtoClientOUT);

            response(clientOperationId, operationId, executionClientTime, request, null);

            throw e;
        } finally {
            MDC.remove(Constants.MDC_CLIENT_OPERATION_ID);
            MDC.remove(Constants.MDC_CLIENT_EXECUTION_TIME);
            MDC.remove(Constants.MDC_EVENT_SUB_CATEGORY);
            MDC.remove(Constants.MDC_CALL_TYPE);
            MDC.remove(Constants.MDC_EVENT_CATEGORY);
            MDC.remove(Constants.MDC_STATUS_CODE);
            MDC.remove(Constants.MDC_CLIENT_TYPE);
        }

        return response;
    }

    public String createRequestMessage(String clientOperationId, String operationId, HttpRequest request, byte[] reqBody) {
        StringBuilder msg = new StringBuilder();
        msg.append(String.format(REQUEST_DEFAULT_MESSAGE_PREFIX, operationId, clientOperationId));
        if (this.requestPretty) {
            msg.append(PRETTY_OUT).append(SPACE);
        }
        msg.append("path: ").append(request.getMethod()).append(' ');
        msg.append(request.getURI());

        if (this.requestIncludeHeaders) {
            HttpHeaders headers = new HttpHeaders();
            request.getHeaders().forEach((s, h) -> {
                headers.add(s, StringUtils.join(h, ","));
            });
            if (this.requestHeaderPredicate != null) {
                headers.forEach(
                        (key, value) -> {
                            if (!this.requestHeaderPredicate.test(key)) {
                                headers.set(key, "masked");
                            }
                        });
            }
            String formatRequestHeaders = formatRequestHeaders(headers);
            if (formatRequestHeaders != null) {
                if (this.requestPretty) {
                    msg.append(PRETTY_OUT).append(SPACE);
                } else {
                    msg.append(", ");
                }
                msg.append("headers: [").append(formatRequestHeaders);
                if (this.requestPretty) {
                    msg.append(PRETTY_OUT).append(SPACE);
                }
                msg.append("]");
            }
        }

        if (this.requestIncludePayload) {
            String payload = new String(reqBody, StandardCharsets.UTF_8);
            if (payload != null) {
                if (this.requestPretty) {
                    msg.append(PRETTY_OUT).append(SPACE);
                } else {
                    msg.append(", ");
                }
                msg.append("payload: ").append(payload);
            }
        }

        return msg.toString();
    }

    public String createResponseMessage(String clientOperationId, String operationId, String clientExecutionTime, HttpRequest request, ClientHttpResponse response)
            throws IOException {
        StringBuilder msg = new StringBuilder();
        msg.append(String.format(RESPONSE_DEFAULT_MESSAGE_PREFIX, operationId, clientOperationId));
        if (this.responsePretty) {
            msg.append(PRETTY_IN).append(SPACE);
        }
        msg.append("path: ").append(request.getMethod()).append(' ');
        msg.append(request.getURI());

        if (this.responsePretty) {
            msg.append(PRETTY_IN).append(SPACE);
        } else {
            msg.append(", ");
        }
        msg.append("client-execution-time: ").append(clientExecutionTime).append("ms");

        if (response != null) {
            if (this.responsePretty) {
                msg.append(PRETTY_IN).append(SPACE);
            }
            msg.append("status: ").append(response.getStatusCode().value());

            if (this.responseIncludeHeaders) {
                HttpHeaders headers = new HttpHeaders();
                response.getHeaders().forEach((s, h) -> {
                    headers.add(s, StringUtils.join(h, ","));
                });
                if (this.responseHeaderPredicate != null) {
                    headers.forEach(
                            (key, value) -> {
                                if (!this.requestHeaderPredicate.test(key)) {
                                    headers.set(key, "masked");
                                }
                            });
                }
                String formatResponseHeaders = formatResponseHeaders(headers);
                if (formatResponseHeaders != null) {
                    if (this.requestPretty) {
                        msg.append(PRETTY_IN).append(SPACE);
                    } else {
                        msg.append(", ");
                    }
                    msg.append("headers: [").append(formatResponseHeaders);
                    if (this.requestPretty) {
                        msg.append(PRETTY_OUT).append(SPACE);
                    }
                    msg.append("]");
                }
            }

            if (this.responseIncludePayload) {
                String payload = bodyToString(response.getBody());
                if (!payload.isBlank()) {
                    if (this.requestPretty) {
                        msg.append(PRETTY_IN).append(SPACE);
                    } else {
                        msg.append(", ");
                    }
                    msg.append("payload: ").append(payload);
                }
            }
        } else {
            if (this.responsePretty) {
                msg.append(PRETTY_IN).append(SPACE);
            }
            msg.append("NO RICEVUTA");
        }

        return msg.toString();
    }


    protected abstract void request(String clientOperationId, String operationId, HttpRequest request, byte[] reqBody);

    protected abstract void response(String clientOperationId, String operationId, String clientExecutionTime, HttpRequest request, ClientHttpResponse response);


    private String formatRequestHeaders(MultiValueMap<String, String> headers) {
        Stream<String> stream = headers.entrySet().stream()
                .map((entry) -> {
                    if (this.requestPretty) {
                        String values = entry.getValue().stream().collect(Collectors.joining("\", \"", "\"", "\""));
                        return PRETTY_OUT + "*\t" + entry.getKey() + ": [" + values + "]";
                    } else {
                        String values = entry.getValue().stream().collect(Collectors.joining("\", \"", "\"", "\""));
                        return entry.getKey() + ": [" + values + "]";
                    }
                });
        if (this.requestPretty) {
            return stream.collect(Collectors.joining(""));
        } else {
            return stream.collect(Collectors.joining(", "));
        }
    }

    private String formatResponseHeaders(MultiValueMap<String, String> headers) {
        Stream<String> stream = headers.entrySet().stream()
                .map((entry) -> {
                    if (this.responsePretty) {
                        String values = entry.getValue().stream().collect(Collectors.joining("\", \"", "\"", "\""));
                        return PRETTY_IN + "*\t" + entry.getKey().toLowerCase() + ": [" + values + "]";
                    } else {
                        String values = entry.getValue().stream().collect(Collectors.joining("\", \"", "\"", "\""));
                        return entry.getKey().toLowerCase() + ": [" + values + "]";
                    }
                });
        if (this.requestPretty) {
            return stream.collect(Collectors.joining(""));
        } else {
            return stream.collect(Collectors.joining(", "));
        }
    }

    private String bodyToString(InputStream body) throws IOException {
        return StreamUtils.copyToString(body, StandardCharsets.UTF_8);
    }

    private void persistInterfaceEventInRE(ReEventDto reEvent) {
        if (this.mustPersistEventOnRE) {
            reService.addRe(reEvent);
        }
    }

    protected void avoidEventPersistenceOnRE() {
        this.mustPersistEventOnRE = false;
    }
}
