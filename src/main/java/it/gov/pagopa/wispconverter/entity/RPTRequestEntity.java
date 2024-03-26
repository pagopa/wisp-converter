package it.gov.pagopa.wispconverter.entity;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import lombok.*;
import org.springframework.data.annotation.Id;

@Container(containerName = "data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RPTRequestEntity {

    @Id
    private String id;

    @PartitionKey
    private String partitionKey;

    private String primitive;

    private String payload;
}
