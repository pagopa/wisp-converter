package it.gov.pagopa.wispconverter.repository.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import lombok.Builder;
import lombok.Data;

@Container(containerName = "re")
@Data
@Builder(toBuilder = true)
public class SessionIdEntity {
    String sessionId;
}
