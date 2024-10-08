package it.gov.pagopa.wispconverter.repository.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import it.gov.pagopa.wispconverter.repository.model.enumz.IdempotencyStatusEnum;
import it.gov.pagopa.wispconverter.repository.model.enumz.ReceiptTypeEnum;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Container(containerName = "idempotency_key")
@Data
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
public class IdempotencyKeyEntity {

    @Id
    private String id;

    @PartitionKey
    private String partitionKey;

    private ReceiptTypeEnum receiptType;

    private String sessionId;

    private IdempotencyStatusEnum status;

    private Instant lockedAt;
}
