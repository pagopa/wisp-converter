package it.gov.pagopa.wispconverter.service.mapper;

import it.gov.pagopa.wispconverter.service.model.RPTContentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DebtPositionMapper {

    it.gov.pagopa.gpdclient.model.PaymentPositionModelDto toPaymentPosition(RPTContentDTO rptContentDTO);
}
