package it.gov.pagopa.wispconverter.service.mapper;

import it.gov.pagopa.wispconverter.repository.model.ReEventEntity;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
