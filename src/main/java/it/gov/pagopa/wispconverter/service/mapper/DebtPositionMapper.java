package it.gov.pagopa.wispconverter.service.mapper;

import it.gov.pagopa.wispconverter.client.gpd.model.PaymentOption;
import it.gov.pagopa.wispconverter.client.gpd.model.PaymentPosition;
import it.gov.pagopa.wispconverter.client.gpd.model.Stamp;
import it.gov.pagopa.wispconverter.service.model.CommonRPTFieldsDTO;
import it.gov.pagopa.wispconverter.service.model.DigitalStampDTO;
import it.gov.pagopa.wispconverter.service.model.RPTContentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DebtPositionMapper {

    @Mapping(source = "payerType", target = "type")
    @Mapping(source = "payerFiscalCode", target = "fiscalCode")
    @Mapping(source = "payerFullName", target = "fullName")
    @Mapping(source = "payerAddressStreetName", target = "streetName")
    @Mapping(source = "payerAddressStreetNumber", target = "civicNumber")
    @Mapping(source = "payerAddressPostalCode", target = "postalCode")
    @Mapping(source = "payerAddressCity", target = "city")
    @Mapping(source = "payerAddressProvince", target = "province")
    @Mapping(source = "payerAddressNation", target = "country")
    @Mapping(source = "payerEmail", target = "email")
    @Mapping(source = "payerFullName", target = "companyName")
    @Mapping(target = "validityDate", expression = "java(null)")
    @Mapping(target = "switchToExpired", constant = "true")
    PaymentPosition toPaymentPosition(CommonRPTFieldsDTO commonRPTFieldsDTO);

    @Mapping(source = "iuv", target = "iuv")
    @Mapping(target = "description", constant = "-")
    @Mapping(target = "isPartialPayment", constant = "false")
    @Mapping(target = "retentionDate", expression = "java(null)")
    @Mapping(target = "fee", constant = "0L")
    @Mapping(target = "notificationFee", constant = "0L")
    @Mapping(target = "dueDate", expression = "java(java.time.LocalDateTime.now().plusDays(1))")
    PaymentOption toPaymentOption(RPTContentDTO rptContentDTO);

    @Mapping(target = "hashDocument", expression = "java(new String(digitalStampDTO.getDocumentHash()))")
    @Mapping(source = "type", target = "stampType")
    @Mapping(source = "province", target = "provincialResidence")
    Stamp toStamp(DigitalStampDTO digitalStampDTO);
}
