package it.gov.pagopa.wispconverter.service;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceBusService {

    @Value("${azure.sb.connectionString}")
    private String connectionString;

    @Value("${azure.sb.paaInviaRT.name}")
    private String queueName;

    @Autowired
    private ServiceBusSenderClient serviceBusSenderClient;

    public void sendMessage(String message) {
        ServiceBusMessage serviceBusMessage = new ServiceBusMessage(message);
        log.debug("Sending message {} to the queue: {}", message, queueName);
        serviceBusSenderClient.sendMessage(serviceBusMessage);
        log.debug("Sent message {} to the queue: {}", message, queueName);
    }
}
