package it.gov.pagopa.wispconverter.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import it.gov.pagopa.wispconverter.repository.model.ReEventEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface ReEventRepository extends CosmosRepository<ReEventEntity, String> {

}