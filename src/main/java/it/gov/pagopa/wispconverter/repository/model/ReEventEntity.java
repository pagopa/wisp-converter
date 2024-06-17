package it.gov.pagopa.wispconverter.repository.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import it.gov.pagopa.wispconverter.repository.model.enumz.*;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Container(containerName = "re")
@Data
@Builder(toBuilder = true)
public class ReEventEntity {

    /**
     * The identifier of the single event.
     */
    @Id
    private String id;

    /**
     * The partition key that refers to the single partition on which the event will be stored.
     */
    @PartitionKey
    private String partitionKey;

    private String requestId;
    private String operationId;
    private String clientOperationId;
    private ComponentEnum component;
    private Instant insertedTimestamp;
    private EventCategoryEnum eventCategory;
    private EventSubcategoryEnum eventSubcategory;
    private CallTypeEnum callType;
    private String consumer;
    private String provider;
    private OutcomeEnum outcome;
    private String httpMethod;
    private String httpUri;
    private String httpHeaders;
    private String httpCallRemoteAddress;
    private Integer httpStatusCode;
    private Long executionTimeMs;
    private String compressedPayload;
    private Integer compressedPayloadLength;
    private String businessProcess;
    private String operationStatus;
    private String operationErrorTitle;
    private String operationErrorDetail;
    private String operationErrorCode;
    private String primitive;
    private String sessionId;
    private String cartId;
    private String iuv;
    private String noticeNumber;
    private String domainId;
    private String ccp;
    private String psp;
    private String station;
    private String channel;
    private String status;
    private String paymentToken;
    private String info;
}
