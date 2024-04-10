package it.gov.pagopa.wispconverter.service.mapper;

import it.gov.pagopa.wispconverter.repository.model.ReEventEntity;
import it.gov.pagopa.wispconverter.service.model.CommonRPTFieldsDTO;
import it.gov.pagopa.wispconverter.service.model.PaymentNoticeContentDTO;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import org.mapstruct.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReEventMapper {

    static final String PATTERN_FORMAT = "yyyy-MM-dd";

    @Mapping(source = "insertedTimestamp", target = "partitionKey", qualifiedByName = "partitionKeyFromInstant")
    ReEventEntity toReEventEntity(ReEventDto reEventDto);

    @Named("partitionKeyFromInstant")
    public static String partitionKeyFromInstant(Instant insertedTimestamp) {
        return insertedTimestamp == null ? null : DateTimeFormatter
                .ofPattern(PATTERN_FORMAT)
                .withZone(ZoneId.systemDefault())
                .format(insertedTimestamp);
    }

}
