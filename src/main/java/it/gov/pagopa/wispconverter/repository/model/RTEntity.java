package it.gov.pagopa.wispconverter.repository.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import it.gov.pagopa.wispconverter.repository.model.enumz.ReceiptTypeEnum;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.Id;

@Container(containerName = "receipts-rt")
@Data
@ToString(exclude = "rt")
@EqualsAndHashCode(exclude = "rt")
@Builder(toBuilder = true)
public class RTEntity {

    @Id
    private String id;

    @PartitionKey
    private String partitionKey;

    private String idDominio;

    private String iuv;

    private String ccp;

    private ReceiptTypeEnum receiptType;

    // ricevuta-telematica-base-64
    private String rt;

    private Long rtTimestamp;
}
