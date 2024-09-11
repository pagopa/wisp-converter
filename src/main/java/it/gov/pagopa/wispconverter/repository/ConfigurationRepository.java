package it.gov.pagopa.wispconverter.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import it.gov.pagopa.wispconverter.repository.model.ConfigurationEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigurationRepository extends CosmosRepository<ConfigurationEntity, String> {
}
