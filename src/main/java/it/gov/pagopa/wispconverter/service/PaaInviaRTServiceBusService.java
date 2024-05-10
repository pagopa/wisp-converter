package it.gov.pagopa.wispconverter.service;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.service.model.re.EntityStatusEnum;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ReUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static it.gov.pagopa.wispconverter.util.Constants.NODO_DEI_PAGAMENTI_SPC;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaaInviaRTServiceBusService {

    @Value("${azure.sb.connectionString}")
    private String connectionString;

    @Value("${azure.sb.paaInviaRT.name}")
    private String queueName;

    public void sendMessage(String message, String subject) {
        // create a token using the default Azure credential
        ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildClient();

        // send one message to the queue
        ServiceBusMessage serviceBusMessage = new ServiceBusMessage(message);
        serviceBusMessage.setSubject(subject);
        log.debug("Sending message to the queue: {}", queueName);
        senderClient.sendMessage(serviceBusMessage);
        log.debug("Sent message to the queue: {}", queueName);
    }
}
