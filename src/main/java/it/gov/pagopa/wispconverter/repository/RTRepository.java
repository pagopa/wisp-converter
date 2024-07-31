package it.gov.pagopa.wispconverter.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import it.gov.pagopa.wispconverter.repository.model.RTEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface RTRepository extends CosmosRepository<RTEntity, String> {

}
