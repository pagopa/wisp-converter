package it.gov.pagopa.wispconverter.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import it.gov.pagopa.wispconverter.repository.model.RTEntity;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RTRepository extends CosmosRepository<RTEntity, String> {


    @Query("SELECT * FROM c WHERE IS_NULL(c.rt) AND c.idDominio = @organizationId AND c._ts >= DateTimeToTimestamp(@dateFrom) / 1000 and c._ts <= DateTimeToTimestamp(@dateTo) / 1000")
    List<RTEntity> findByOrganizationId(@Param("organizationId") String organizationId,
                                        @Param("dateFrom") String dateFrom,
                                        @Param("dateTo") String dateTo);
}
