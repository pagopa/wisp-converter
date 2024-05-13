package it.gov.pagopa.wispconverter;

import com.azure.messaging.servicebus.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
//@EnableJms
public class Application {

    @Value("${azure.sb.connectionString}")
    private String connectionString;

    @Value("${azure.sb.paaInviaRT.name}")
    private String queueName;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ServiceBusSenderClient senderClient(){
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildClient();
    }

}
