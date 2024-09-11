package it.gov.pagopa.wispconverter.repository.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import it.gov.pagopa.wispconverter.repository.model.enumz.ReceiptTypeEnum;
import lombok.*;
import org.springframework.data.annotation.Id;

@Container(containerName = "configuration")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationEntity {

    @Id
    private String id;

}
