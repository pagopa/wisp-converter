package it.gov.pagopa.wispconverter.service.mapper;

import it.gov.pagopa.wispconverter.service.model.session.PaymentNoticeContentDTO;
import it.gov.pagopa.wispconverter.service.model.session.SessionDataDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartMapper {

    @Mapping(source = "commonFields.sessionId", target = "idCart")
    @Mapping(source = "commonFields.payerEmail", target = "emailNotice")
    @Mapping(target = "allCCP", constant = "false")
    @Mapping(target = "paymentNotices", ignore = true)
    it.gov.pagopa.gen.wispconverter.client.checkout.model.CartRequestDto toCart(SessionDataDTO sessionData);


    it.gov.pagopa.gen.wispconverter.client.checkout.model.PaymentNoticeDto toPaymentNotice(PaymentNoticeContentDTO paymentNoticeContentDTO);
}
