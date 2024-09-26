package it.gov.pagopa.wispconverter.repository.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import it.gov.pagopa.wispconverter.repository.model.enumz.ReceiptStatusEnum;
import it.gov.pagopa.wispconverter.repository.model.enumz.ReceiptTypeEnum;
import lombok.*;
import org.springframework.data.annotation.Id;

@Container(containerName = "receipts-rt")
@Data
@ToString(exclude = "rt")
@EqualsAndHashCode(exclude = "rt")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RTEntity {

    @Id
    private String id; // todo is useful update id adding sessionId?

    @PartitionKey
    private String partitionKey;

    private String idDominio;

    private String iuv;

    private String ccp;

    private String sessionId;

    private ReceiptStatusEnum receiptStatus;

    private ReceiptTypeEnum receiptType;

    // ricevuta-telematica-base-64
    private String rt;

    private Long rtTimestamp;
}
