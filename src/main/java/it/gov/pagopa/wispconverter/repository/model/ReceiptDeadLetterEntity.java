package it.gov.pagopa.wispconverter.repository.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Container(containerName = "receipt-dead-letter")
@Data
@ToString
@EqualsAndHashCode(callSuper=false)
@SuperBuilder(toBuilder = true)
public class ReceiptDeadLetterEntity extends RTRequestEntity {

}
