package it.gov.pagopa.wispconverter.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RPTRequestRepository extends CosmosRepository<RPTRequestEntity, String> {

    Optional<RPTRequestEntity> findById(String id);
}