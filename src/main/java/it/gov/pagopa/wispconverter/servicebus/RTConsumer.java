package it.gov.pagopa.wispconverter.servicebus;

import com.azure.cosmos.models.PartitionKey;
import com.azure.messaging.servicebus.*;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.RTRequestRepository;
import it.gov.pagopa.wispconverter.repository.model.RTRequestEntity;
import it.gov.pagopa.wispconverter.service.ConfigCacheService;
import it.gov.pagopa.wispconverter.service.PaaInviaRTService;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import java.util.Map;
import java.time.ZonedDateTime;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RTConsumer extends SBConsumer {

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
    public void initializeClient() {
        if(receiverClient!=null){
            log.info("[Scheduled] Starting RTConsumer {}", ZonedDateTime.now());
            receiverClient.start();
        }
    }

    @PostConstruct
    public void post(){
    if (StringUtils.isNotBlank(connectionString) && !connectionString.equals("-")) {
      receiverClient =
          new ServiceBusClientBuilder()
              .connectionString(connectionString)
              .processor()
              .queueName(queueName)
              .processMessage(this::processMessage)
              .processError(this::processError)
              .buildProcessorClient();
        }
    }

    @PreDestroy
    public void preDestroy(){
        receiverClient.close();
    }

    public void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        log.info("Processing "+message.getMessageId());
        try{
            String cosmosId = new String(message.getBody().toBytes());
            String station = message.getSubject();
            String[] idparts = cosmosId.split("_");
            String pk = idparts[0];
            String receiptId = idparts[1]+"_"+idparts[2];
            Optional<RTRequestEntity> byId = rtRequestRepository.findById(receiptId,new PartitionKey(pk));
            log.debug(byId.toString());
            byId.ifPresent(receipt->{

                if(receipt.getRetry()>=48){
                    log.warn("Max retry reached for message {}", cosmosId);
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

                    boolean ok = false;
                    try{
                        log.debug("[{}]Sending receipt",receiptId);
                        paaInviaRTService.send(url,receipt.getPayload());
                        ok = true;
                    } catch (AppException appe){
                        log.error("[{}]error while sending receipt:{}",receipt,appe.toString());
                        ok = false;
                    }
                    if(ok){
                        log.info("[{}]Removing sent receipt",receiptId);
                        rtRequestRepository.delete(receipt);
                    }else{
                        log.debug("[{}]Increasing retry and saving", receiptId);
                        receipt.setRetry(receipt.getRetry()+1);
                        rtRequestRepository.save(receipt);
                        ServiceBusMessage serviceBusMessage = new ServiceBusMessage(message.getBody());
                        serviceBusMessage.setScheduledEnqueueTime(ZonedDateTime.now().plusHours(1).toOffsetDateTime());
                        log.debug("[{}]Rescheduling receipt at {}", receiptId,serviceBusMessage.getScheduledEnqueueTime());
                        serviceBusSenderClient.sendMessage(serviceBusMessage);
                        log.debug("[{}]Rescheduled receipt at {}", receiptId,serviceBusMessage.getScheduledEnqueueTime());
                    }
                }
            });
        } catch (Exception e){
            log.error("Generic error while processing message",e);
        }
    }
}
