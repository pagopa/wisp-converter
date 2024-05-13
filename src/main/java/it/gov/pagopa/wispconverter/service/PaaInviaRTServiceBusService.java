package it.gov.pagopa.wispconverter.service;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaaInviaRTServiceBusService {

    @Value("${azure.sb.connectionString}")
    private String connectionString;

    @Value("${azure.sb.paaInviaRT.name}")
    private String queueName;

    public void sendMessage(String message) {
        // create a token using the default Azure credential
        ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildClient();

        // send one message to the queue
        ServiceBusMessage serviceBusMessage = new ServiceBusMessage(message);
        log.debug("Sending message {} to the queue: {}", message, queueName);
        senderClient.sendMessage(serviceBusMessage);
        log.debug("Sent message {} to the queue: {}", message, queueName);
    }
}
