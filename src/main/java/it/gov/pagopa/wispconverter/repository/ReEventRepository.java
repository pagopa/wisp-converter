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
            "AND c.status = @status")
    List<ReEventEntity> findBySessionIdAndStatus(@Param("dateFrom") String dateFrom,
                                                 @Param("dateTo") String dateTo,
                                                 @Param("sessionId") String sessionId,
                                                 @Param("status") String status);


    @Query(
          "SELECT wispSession.sessionId " +
                  "FROM ( " +
                  "SELECT c.sessionId, COUNT(1) AS occurrences " +
                  "FROM c " +
                  "WHERE c._ts * 1000 >= DateTimeToTimestamp(@dateFrom) " +
                  "AND c._ts * 1000 < DateTimeToTimestamp(@dateTo) " +
                  "AND ( " +
                  "(c.component = 'WISP_SOAP_CONVERTER' AND c.eventCategory = 'INTERFACE' AND c.eventSubcategory = 'RESP' AND c.primitive != 'nodoChiediCopiaRT') " +
                  "OR " +
                  "(c.component = 'WISP_CONVERTER' AND c.businessProcess = 'redirect' AND c.status = 'FOUND_RPT_IN_STORAGE')" +
                  " ) " +
                  "GROUP BY c.sessionId " +
                  ") AS wispSession " +
                  "WHERE wispSession.occurrences = 1"
    )
    List<String> findSessionWithoutRedirect(@Param("dateFrom") String dateFrom, @Param("dateTo") String dateTo);
}