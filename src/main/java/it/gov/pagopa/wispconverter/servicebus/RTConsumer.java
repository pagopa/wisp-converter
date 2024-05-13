package it.gov.pagopa.wispconverter.servicebus;

import com.azure.cosmos.models.PartitionKey;
import com.azure.messaging.servicebus.*;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.RTRequestRepository;
import it.gov.pagopa.wispconverter.repository.model.RTRequestEntity;
import it.gov.pagopa.wispconverter.service.ConfigCacheService;
import it.gov.pagopa.wispconverter.service.PaaInviaRTService;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RTConsumer {

    @Value("${azure.sb.connectionString}")
    private String connectionString;

    @Value("${azure.sb.paaInviaRT.name}")
    private String queueName;

    @Autowired
    private RTRequestRepository rtRequestRepository;
    @Autowired
    private ServiceBusSenderClient serviceBusSenderClient;
    @Autowired
    private PaaInviaRTService paaInviaRTService;
    @Autowired
    private ConfigCacheService configCacheService;

    private ServiceBusProcessorClient receiverClient;

    @EventListener(ApplicationReadyEvent.class)
    public void refreshCache() {
        log.info("[Scheduled] Starting RTConsumer {}", ZonedDateTime.now());
        receiverClient.start();
    }

    @PostConstruct
    public void post(){
        receiverClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .queueName(queueName)
                .processMessage(context -> processMessage(context))
                .processError(context -> processError(context))
                .buildProcessorClient();

    }

    @PreDestroy
    public void preDestroy(){
        receiverClient.close();
    }

    public void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        try{
            String cosmosId = new String(message.getBody().toBytes());
            String station = message.getSubject();
            String[] idparts = cosmosId.split("_");
            String pk = idparts[0];
            String id = idparts[1]+"_"+idparts[2];
            Optional<RTRequestEntity> byId = rtRequestRepository.findById(id,new PartitionKey(pk));
            log.debug(byId.toString());
            byId.ifPresent(receipt->{

                if(receipt.getRetry()>48){
                    log.debug("Max retry reached for message {}", cosmosId);
                }else{
                    log.debug("Sending message {},retry: {}", cosmosId,receipt.getRetry());
                    Map<String, it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto> stations = configCacheService.getConfigData().getStations();
                    it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto stationDto = stations.get(station);
                    String url = CommonUtility.constructUrl(
                            stationDto.getConnection().getProtocol().getValue(),
                            stationDto.getConnection().getIp(),
                            stationDto.getConnection().getPort().intValue(),
                            stationDto.getService().getPath(),
                            null,
                            null
                    );

                    Boolean ok = false;
                    try{
                        paaInviaRTService.send(url,receipt.getPayload());
                        ok = true;
                    } catch (AppException appe){
                        ok = false;
                    }
                    if(ok){

                    }else{
                        receipt.setRetry(receipt.getRetry()+1);
                        rtRequestRepository.save(receipt);
                        ServiceBusMessage serviceBusMessage = new ServiceBusMessage(message.getBody());
                        serviceBusMessage.setScheduledEnqueueTime(ZonedDateTime.now().plusSeconds(10).toOffsetDateTime());
                        serviceBusSenderClient.sendMessage(serviceBusMessage);
                        log.debug("Rescheduled receipt {} at {}", cosmosId,serviceBusMessage.getScheduledEnqueueTime());
                    }
                }
            });
        } catch (Exception e){
            log.error("Generic error while processing message",e);
        }
    }

    public void processError(ServiceBusErrorContext context) {
        System.out.printf("Error when receiving messages from namespace: '%s'. Entity: '%s'%n",
                context.getFullyQualifiedNamespace(), context.getEntityPath());

        if (!(context.getException() instanceof ServiceBusException)) {
            System.out.printf("Non-ServiceBusException occurred: %s%n", context.getException());
            return;
        }

        ServiceBusException exception = (ServiceBusException) context.getException();
        ServiceBusFailureReason reason = exception.getReason();

        if (reason == ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED
                || reason == ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND
                || reason == ServiceBusFailureReason.UNAUTHORIZED) {
            System.out.printf("An unrecoverable error occurred. Stopping processing with reason %s: %s%n",
                    reason, exception.getMessage());
        } else if (reason == ServiceBusFailureReason.MESSAGE_LOCK_LOST) {
            System.out.printf("Message lock lost for message: %s%n", context.getException());
        } else if (reason == ServiceBusFailureReason.SERVICE_BUSY) {
            try {
                // Choosing an arbitrary amount of time to wait until trying again.
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                System.err.println("Unable to sleep for period of time");
            }
        } else {
            System.out.printf("Error source %s, reason %s, message: %s%n", context.getErrorSource(),
                    reason, context.getException());
        }
    }
}