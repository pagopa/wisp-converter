package it.gov.pagopa.wispconverter.client.decoupler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecouplerCachingKeys {

    private List<String> keys;
}
