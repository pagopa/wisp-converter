package it.gov.pagopa.wispconverter.service.mapper;

import it.gov.pagopa.wispconverter.service.model.CommonRPTFieldsDTO;
import it.gov.pagopa.wispconverter.service.model.PaymentNoticeContentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartMapper {

    @Mapping(source = "cartId", target = "idCart")
    @Mapping(source = "payerEmail", target = "emailNotice")
    @Mapping(target = "allCCP", constant = "false")
    it.gov.pagopa.gen.wispconverter.client.checkout.model.CartRequestDto toCart(CommonRPTFieldsDTO commonRPTFieldsDTO);


    it.gov.pagopa.gen.wispconverter.client.checkout.model.PaymentNoticeDto toPaymentNotice(PaymentNoticeContentDTO paymentNoticeContentDTO);
}
