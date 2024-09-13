package it.gov.pagopa.wispconverter.servicebus;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wispconverter.controller.model.RPTTimerRequest;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.model.enumz.IdempotencyStatusEnum;
import it.gov.pagopa.wispconverter.repository.model.enumz.InternalStepStatus;
import it.gov.pagopa.wispconverter.repository.model.enumz.ReceiptTypeEnum;
import it.gov.pagopa.wispconverter.service.ConfigCacheService;
import it.gov.pagopa.wispconverter.service.IdempotencyService;
import it.gov.pagopa.wispconverter.service.PaaInviaRTSenderService;
import it.gov.pagopa.wispconverter.service.ReceiptService;
import it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto;
import it.gov.pagopa.gen.wispconverter.client.cache.model.ConnectionDto;
import it.gov.pagopa.wispconverter.service.model.ReceiptDto;
import it.gov.pagopa.wispconverter.service.model.session.RPTContentDTO;
import it.gov.pagopa.wispconverter.service.model.session.SessionDataDTO;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import it.gov.pagopa.wispconverter.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * This class is the consumer of the queue 'ecommerce-hang-timeout' of the service bus.
 * When a schedule message trigger this class we'll expire the payment,
 * and we'll send a sendRT-Negative to the Creditor Institution
 */
@Slf4j
@Component
public class RPTTimeoutConsumer extends SBConsumer {

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${azure.sb.wisp-rpt-timeout-queue.connectionString}")
    private String connectionString;

    @Value("${azure.sb.queue.wisp-rpt-timeout.name}")
    private String queueName;

    @Value("${wisp-converter.station-in-forwarder.partial-path}")
    private String stationInForwarderPartialPath;

    @Value("${wisp-converter.forwarder.api-key}")
    private String forwarderSubscriptionKey;

    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private PaaInviaRTSenderService paaInviaRTSenderService;

    @Autowired
    private ConfigCacheService configCacheService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private it.gov.pagopa.gen.wispconverter.client.decouplercaching.invoker.ApiClient decouplerCachingClient;


    @PostConstruct
    public void post() {
        if (StringUtils.isNotBlank(connectionString) && !connectionString.equals("-")) {
            receiverClient = CommonUtility.getServiceBusProcessorClient(connectionString, queueName, this::processMessage, this::processError);
        }
    }


    @EventListener(ApplicationReadyEvent.class)
    public void initializeClient() {
        if (receiverClient != null) {
            log.info("[Scheduled] Starting RPTTimeoutConsumer {}", ZonedDateTime.now());
            receiverClient.start();
        }
    }

    public void processMessage(ServiceBusReceivedMessageContext context) {
        setSessionDataInfoInMDC("rpt-timeout-trigger");
        ServiceBusReceivedMessage message = context.getMessage();
        log.info("Processing message. Session: {}, Sequence #: {}. Contents: {}", message.getMessageId(), message.getSequenceNumber(), message.getBody());
        try {
            // read the message
            RPTTimerRequest timeoutMessage = mapper.readValue(message.getBody().toStream(), RPTTimerRequest.class);

            // deactivate the sessionId inside the cache
            it.gov.pagopa.gen.wispconverter.client.decouplercaching.api.DefaultApi apiInstance = new it.gov.pagopa.gen.wispconverter.client.decouplercaching.api.DefaultApi(decouplerCachingClient);
            it.gov.pagopa.gen.wispconverter.client.decouplercaching.model.SessionIdDto sessionIdDto = new it.gov.pagopa.gen.wispconverter.client.decouplercaching.model.SessionIdDto();
            sessionIdDto.setSessionId(timeoutMessage.getSessionId());
            apiInstance.deleteSessionId(sessionIdDto, MDC.get(Constants.MDC_REQUEST_ID));

            // log event
            MDC.put(Constants.MDC_SESSION_ID, timeoutMessage.getSessionId());
            generateRE(InternalStepStatus.RPT_TIMER_TRIGGER, "Expired rpt timer. A Negative sendRT will be sent: " + timeoutMessage);

            SessionDataDTO sessionDataDTO = receiptService.getSessionDataFromSessionId(timeoutMessage.getSessionId());

            gov.telematici.pagamenti.ws.papernodo.ObjectFactory objectFactory = new gov.telematici.pagamenti.ws.papernodo.ObjectFactory();

            // retrieve configuration data from cache
            it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigDataV1Dto configData = configCacheService.getConfigData();
            Map<String, it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigurationKeyDto> configurations = configData.getConfigurations();
            Map<String, StationDto> stations = configData.getStations();

            for(RPTContentDTO rpt: sessionDataDTO.getRpts().values()) {
                // idempotency key creation to check if the rt has already been sent
                String idempotencyKey = sessionDataDTO.getCommonFields().getSessionId() + "_" + null;
                String rtRawPayload = receiptService.generateKoRtFromSessionData(
                        sessionDataDTO.getCommonFields().getCreditorInstitutionId(),
                        rpt.getIuv(),
                        rpt,
                        sessionDataDTO.getCommonFields(),
                        objectFactory,
                        configurations);
                boolean isSuccessful = false;
                StationDto station = stations.get(sessionDataDTO.getCommonFields().getStationId());
                ConnectionDto stationConnection = station.getConnection();
                String url = CommonUtility.constructUrl(
                        stationConnection.getProtocol().getValue(),
                        stationConnection.getIp(),
                        stationConnection.getPort().intValue(),
                        station.getService() != null ? station.getService().getPath() : "",
                        null,
                        null
                );
                List<Pair<String, String>> headers = CommonUtility.constructHeadersForPaaInviaRT(url, station, stationInForwarderPartialPath, forwarderSubscriptionKey);
                IdempotencyStatusEnum idempotencyStatus;
                try {

                    // send the receipt to the creditor institution via the URL set in the station configuration
                    paaInviaRTSenderService.sendToCreditorInstitution(url, headers, rtRawPayload);

                    // generate a new event in RE for store the successful sending of the receipt
                    receiptService.generateREForSentRT(sessionDataDTO, rpt.getIuv(), null);
                    idempotencyStatus = IdempotencyStatusEnum.SUCCESS;
                    isSuccessful = true;

                } catch (Exception e) {

                    // generate a new event in RE for store the unsuccessful sending of the receipt
                    String messageException = e.getMessage();
                    if (e instanceof AppException appException) {
                        messageException = appException.getError().getDetail();
                    }

                    log.error("Exception: " + AppErrorCodeMessageEnum.RECEIPT_KO_NOT_GENERATED_BUT_MAYBE_RESCHEDULED.getDetail());
                    receiptService.generateREForNotSentRT(sessionDataDTO, rpt.getIuv(), null, messageException);

                    // because of the not sent receipt, it is necessary to schedule a retry of the sending process for this receipt
                    receiptService.scheduleRTSend(sessionDataDTO, url, headers, rtRawPayload, station, rpt.getIuv(), null, idempotencyKey, ReceiptTypeEnum.KO);
                    idempotencyStatus = IdempotencyStatusEnum.FAILED;
                }

                try {
                    // Unlock idempotency key after a successful operation
                    idempotencyService.unlockIdempotencyKey(idempotencyKey, ReceiptTypeEnum.KO, idempotencyStatus);
                } catch (AppException e) {
                    log.error("AppException: ", e);
                }
            }
        } catch (IOException e) {
            log.error("Error when read rpt timer request value from message: '{}'. Body: '{}'", message.getMessageId(), message.getBody());
        }
        MDC.clear();
    }
}