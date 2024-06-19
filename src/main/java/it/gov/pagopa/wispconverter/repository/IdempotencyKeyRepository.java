package it.gov.pagopa.wispconverter.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import it.gov.pagopa.wispconverter.repository.model.IdempotencyKeyEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyKeyRepository extends CosmosRepository<IdempotencyKeyEntity, String> {

}
