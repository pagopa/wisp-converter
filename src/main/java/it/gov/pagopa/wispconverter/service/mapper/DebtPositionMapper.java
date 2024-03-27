package it.gov.pagopa.wispconverter.service.mapper;

import it.gov.pagopa.wispconverter.client.gpd.model.PaymentPosition;
import it.gov.pagopa.wispconverter.service.model.RPTContentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DebtPositionMapper {

    PaymentPosition toPaymentPosition(RPTContentDTO rptContentDTO);
}
