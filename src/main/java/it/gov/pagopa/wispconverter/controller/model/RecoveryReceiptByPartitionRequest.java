package it.gov.pagopa.wispconverter.controller.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecoveryReceiptByPartitionRequest {

    private List<String> partitionKeys;
}
