package it.gov.pagopa.wispconverter.repository.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Container(containerName = "receipt-dead-letter")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder(toBuilder = true)
public class ReceiptDeadLetterEntity extends RTRequestEntity {

}
