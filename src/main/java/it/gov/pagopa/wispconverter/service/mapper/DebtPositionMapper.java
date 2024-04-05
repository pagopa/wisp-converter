package it.gov.pagopa.wispconverter.service.mapper;

import it.gov.pagopa.wispconverter.client.gpd.model.PaymentOptionModelDto;
import it.gov.pagopa.wispconverter.client.gpd.model.PaymentPositionModelDto;
import it.gov.pagopa.wispconverter.client.gpd.model.StampDto;
import it.gov.pagopa.wispconverter.service.model.CommonRPTFieldsDTO;
import it.gov.pagopa.wispconverter.service.model.DigitalStampDTO;
import it.gov.pagopa.wispconverter.service.model.RPTContentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DebtPositionMapper {

    PaymentPositionModelDto toPaymentPosition(RPTContentDTO rptContentDTO);

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
    PaymentPositionModelDto toPaymentPosition(CommonRPTFieldsDTO commonRPTFieldsDTO);

    @Mapping(source = "iuv", target = "iuv")
    @Mapping(target = "description", constant = "-")
    @Mapping(target = "isPartialPayment", constant = "false")
    @Mapping(target = "retentionDate", expression = "java(null)")
    @Mapping(target = "fee", constant = "0L")
    @Mapping(target = "dueDate", expression = "java(java.time.OffsetDateTime.now().plusDays(1))")
    PaymentOptionModelDto toPaymentOption(RPTContentDTO rptContentDTO);

    @Mapping(target = "hashDocument", expression = "java(new String(digitalStampDTO.getDocumentHash()))")
    @Mapping(source = "type", target = "stampType")
    @Mapping(source = "province", target = "provincialResidence")
    StampDto toStamp(DigitalStampDTO digitalStampDTO);
}
