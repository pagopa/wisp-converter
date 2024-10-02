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
    // these queries check whether the receipt submission has been in an intermediate state (If it has been for more than an hour, it is stuck)

    @Query("SELECT * FROM c WHERE c.receiptStatus in ('SENDING', 'REDIRECT') " +
                   "AND c._ts >= DateTimeToTimestamp(@dateFrom) / 1000 AND c._ts <= DateTimeToTimestamp(@dateTo) / 1000")
    List<RTEntity> findByMidReceiptStatusInAndTimestampBetween(@Param("dateFrom") String dateFrom, @Param("dateTo") String dateTo);

    @Query("SELECT * FROM c WHERE c.receiptStatus in ('SENDING', 'REDIRECT') " +
                   "AND c._ts >= DateTimeToTimestamp(@dateFrom) / 1000 AND c._ts <= DateTimeToTimestamp(@dateTo) / 1000 " +
                   "AND c.domainId = @domainId")
    List<RTEntity> findByMidReceiptStatusInAndTimestampBetween(@Param("dateFrom") String dateFrom,
                                                               @Param("dateTo") String dateTo,
                                                               @Param("domainId") String domainId);


    @Query("SELECT * FROM c WHERE c.receiptStatus in ('SENDING', 'REDIRECT') " +
                   "AND c._ts >= DateTimeToTimestamp(@dateFrom) / 1000 AND c._ts <= DateTimeToTimestamp(@dateTo) / 1000 " +
                   "AND c.iuv = @iuv AND c.domainId = @domainId")
    List<RTEntity> findByMidReceiptStatusInAndTimestampBetween(@Param("dateFrom") String dateFrom,
                                                               @Param("dateTo") String dateTo,
                                                               @Param("domainId") String domainId,
                                                               @Param("iuv") String iuv);
}

