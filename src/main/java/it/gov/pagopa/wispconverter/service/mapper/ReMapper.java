package it.gov.pagopa.wispconverter.service.mapper;

import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReMapper {
    ReMapper INSTANCE = Mappers.getMapper(ReMapper.class);
    ReEventDto clone(ReEventDto reEventDto);
}
