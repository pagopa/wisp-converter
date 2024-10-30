package it.gov.pagopa.wispconverter.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import it.gov.pagopa.wispconverter.repository.model.NavToIuvMappingEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface NavToIuvMappingRepository extends CosmosRepository<NavToIuvMappingEntity, String> {

}
