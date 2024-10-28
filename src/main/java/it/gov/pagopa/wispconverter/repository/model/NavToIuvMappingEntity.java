package it.gov.pagopa.wispconverter.repository.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.Id;

@Container(containerName = "nav2iuv-mapping")
@Data
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
public class NavToIuvMappingEntity {

    @Id
    private String id;

    @PartitionKey
    private String partitionKey;

    private String iuv;
}
