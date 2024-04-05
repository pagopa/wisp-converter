package it.gov.pagopa.wispconverter.service.mapper;

import it.gov.pagopa.wispconverter.client.checkout.model.CartRequestDto;
import it.gov.pagopa.wispconverter.client.checkout.model.PaymentNoticeDto;
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
        //@Mapping(source = "stationId", target = "stationId") TODO to be added on new API version
    CartRequestDto toCart(CommonRPTFieldsDTO commonRPTFieldsDTO);

    @Mapping(source = "noticeNumber", target = "noticeNumber")
    @Mapping(source = "fiscalCode", target = "fiscalCode")
    @Mapping(source = "amount", target = "amount")
    @Mapping(target = "companyName", constant = "null")
    @Mapping(target = "description", constant = "null")
    PaymentNoticeDto toPaymentNotice(PaymentNoticeContentDTO paymentNoticeContentDTO);
}
