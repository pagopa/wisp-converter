package it.gov.pagopa.wispconverter.service.mapper;

import it.gov.pagopa.wispconverter.client.checkout.model.Cart;
import it.gov.pagopa.wispconverter.service.model.CommonRPTFieldsDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartMapper {

    @Mapping(source = "cartId", target = "idCart")
    @Mapping(source = "payerEmail", target = "emailNotice")
    @Mapping(target = "allCCP", constant = "false")
    @Mapping(source = "stationId", target = "stationId")
    Cart toCart(CommonRPTFieldsDTO commonRPTFieldsDTO);
}
