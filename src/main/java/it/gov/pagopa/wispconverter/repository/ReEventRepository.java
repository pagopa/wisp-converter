package it.gov.pagopa.wispconverter.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import it.gov.pagopa.wispconverter.repository.model.ReEventEntity;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReEventRepository extends CosmosRepository<ReEventEntity, String> {

    @Query("SELECT * FROM c " +
            "WHERE (c.partitionKey >= @dateFrom AND c.partitionKey <= @dateTo) " +
            "AND c.iuv = @iuv " +
            "AND c.domainId = @organizationId")
    List<ReEventEntity> findByIuvAndOrganizationId(@Param("dateFrom") String dateFrom,
                                                   @Param("dateTo") String dateTo,
                                                   @Param("iuv") String iuv,
                                                   @Param("organizationId") String organizationId);


    @Query("SELECT * FROM c " +
                   "WHERE (c.partitionKey >= @dateFrom AND c.partitionKey <= @dateTo) " +
                   "AND c.sessionId = @sessionId " +
                   "AND c.status = @status " +
                   "AND ARRAY_CONTAINS(@businessProcesses, c.businessProcess)")
    List<ReEventEntity> findBySessionIdAndStatusAndBusinessProcess(@Param("dateFrom") String dateFrom,
                                                   @Param("dateTo") String dateTo,
                                                   @Param("sessionId") String sessionId,
                                                   @Param("status") String status,
                                                   @Param("businessProcesses") List<String> businessProcesses);
}