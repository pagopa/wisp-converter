package it.gov.pagopa.wispconverter.servicebus;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wispconverter.controller.model.RPTTimerRequest;
import it.gov.pagopa.wispconverter.repository.model.enumz.OutcomeEnum;
import it.gov.pagopa.wispconverter.repository.model.enumz.WorkflowStatus;
import it.gov.pagopa.wispconverter.service.*;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.MDCUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.ZonedDateTime;

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
    private ReService reService;

    @Autowired
    private PaaInviaRTSenderService paaInviaRTSenderService;

    @Autowired
    private ConfigCacheService configCacheService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private it.gov.pagopa.gen.wispconverter.client.decouplercaching.invoker.ApiClient decouplerCachingClient;

    @Value("${disable-service-bus-receiver}")
    private boolean disableServiceBusReceiver;

    @PostConstruct
    public void post() {
        if (StringUtils.isNotBlank(connectionString) && !connectionString.equals("-") && !disableServiceBusReceiver) {
            receiverClient = CommonUtility.getServiceBusProcessorClient(connectionString, queueName, this::processMessage, this::processError);
        }
    }


    @EventListener(ApplicationReadyEvent.class)
    public void initializeClient() {
        if (receiverClient != null && !disableServiceBusReceiver) {
            log.info("[Scheduled] Starting RPTTimeoutConsumer {}", ZonedDateTime.now());
            receiverClient.start();
        }
    }

    public void processMessage(ServiceBusReceivedMessageContext context) {
        MDCUtil.setSessionDataInfo("rpt-timeout-trigger");
        ServiceBusReceivedMessage message = context.getMessage();

        log.info("Processing message. Session: {}, Sequence #: {}. Contents: {}", message.getMessageId(), message.getSequenceNumber(), message.getBody());
        OutcomeEnum outcome = OutcomeEnum.ERROR;
        try {
            // read the message
            RPTTimerRequest timeoutMessage = mapper.readValue(message.getBody().toStream(), RPTTimerRequest.class);
            MDC.put(Constants.MDC_SESSION_ID, timeoutMessage.getSessionId());

            // sending rt- from session id
            receiptService.sendRTKoFromSessionId(timeoutMessage.getSessionId());

            outcome = MDC.get(Constants.MDC_OUTCOME) == null ? OutcomeEnum.OK : OutcomeEnum.valueOf(MDC.get(Constants.MDC_OUTCOME));

        } catch (IOException e) {
            outcome = MDC.get(Constants.MDC_OUTCOME) == null ? OutcomeEnum.ERROR : OutcomeEnum.valueOf(MDC.get(Constants.MDC_OUTCOME));
            log.error("Error when read rpt timer request value from message: '{}'. Body: '{}'", message.getMessageId(), message.getBody());
        } finally {
            reService.sendEvent(WorkflowStatus.RPT_TIMER_IN_TIMEOUT, context.getMessage(), "Triggered timeout ended.", outcome);
        }
    }
}