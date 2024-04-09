package it.gov.pagopa.wispconverter.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import it.gov.pagopa.wispconverter.repository.model.RTRequestEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface RTRequestRepository extends CosmosRepository<RTRequestEntity, String> {

}