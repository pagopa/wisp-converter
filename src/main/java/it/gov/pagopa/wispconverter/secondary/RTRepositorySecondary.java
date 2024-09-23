package it.gov.pagopa.wispconverter.secondary;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import it.gov.pagopa.wispconverter.repository.model.RTEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Qualifier("secondaryCosmosTemplate")
public interface RTRepositorySecondary extends CosmosRepository<RTEntity, String> {


    @Query("SELECT * FROM c WHERE IS_NULL(c.rt) AND c.idDominio = @organizationId AND c._ts >= DateTimeToTimestamp(@dateFrom) / 1000 and c._ts <= DateTimeToTimestamp(@dateTo) / 1000")
    List<RTEntity> findByOrganizationId(@Param("organizationId") String organizationId,
                                        @Param("dateFrom") String dateFrom,
                                        @Param("dateTo") String dateTo);

    @Query("SELECT * FROM c WHERE IS_NULL(c.rt) AND c._ts >= DateTimeToTimestamp(@dateFrom) / 1000 and c._ts <= DateTimeToTimestamp(@dateTo) / 1000")
    List<RTEntity> findPendingRT(@Param("dateFrom") String dateFrom, @Param("dateTo") String dateTo);
}

