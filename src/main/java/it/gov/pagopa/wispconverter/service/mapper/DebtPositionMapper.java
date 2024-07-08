package it.gov.pagopa.wispconverter.service.mapper;

import it.gov.pagopa.wispconverter.service.model.DigitalStampDTO;
import it.gov.pagopa.wispconverter.service.model.session.RPTContentDTO;
import it.gov.pagopa.wispconverter.service.model.session.SessionDataDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DebtPositionMapper {

    @Mapping(source = "rpt.payer.subjectUniqueIdentifier.type", target = "type")
    @Mapping(source = "rpt.payer.subjectUniqueIdentifier.code", target = "fiscalCode")
    @Mapping(source = "rpt.domain.domainName", target = "companyName")
    @Mapping(source = "rpt.payer.name", target = "fullName")
    @Mapping(source = "rpt.payer.address", target = "streetName")
    @Mapping(source = "rpt.payer.streetNumber", target = "civicNumber")
    @Mapping(source = "rpt.payer.postalCode", target = "postalCode")
    @Mapping(source = "rpt.payer.city", target = "city")
    @Mapping(source = "rpt.payer.province", target = "province")
    @Mapping(source = "rpt.payer.nation", target = "country")
    @Mapping(source = "rpt.payer.email", target = "email")
    @Mapping(target = "validityDate", expression = "java(null)")
    @Mapping(target = "switchToExpired", constant = "false")
    @Mapping(target = "payStandIn", constant = "false")
    it.gov.pagopa.gen.wispconverter.client.gpd.model.PaymentPositionModelDto toPaymentPosition(RPTContentDTO rptContentDTO);

    @Mapping(source = "commonFields.payerType", target = "type")
    @Mapping(source = "commonFields.payerFiscalCode", target = "fiscalCode")
    @Mapping(source = "commonFields.payerFullName", target = "fullName")
    @Mapping(source = "commonFields.payerAddressStreetName", target = "streetName")
    @Mapping(source = "commonFields.payerAddressStreetNumber", target = "civicNumber")
    @Mapping(source = "commonFields.payerAddressPostalCode", target = "postalCode")
    @Mapping(source = "commonFields.payerAddressCity", target = "city")
    @Mapping(source = "commonFields.payerAddressProvince", target = "province")
    @Mapping(source = "commonFields.payerAddressNation", target = "country")
    @Mapping(source = "commonFields.payerEmail", target = "email")
    @Mapping(source = "commonFields.creditorInstitutionName", target = "companyName")
    @Mapping(target = "validityDate", expression = "java(null)")
    @Mapping(target = "switchToExpired", constant = "false")
    @Mapping(target = "payStandIn", constant = "false")
    it.gov.pagopa.gen.wispconverter.client.gpd.model.PaymentPositionModelDto toPaymentPosition(SessionDataDTO sessionData);

    @Mapping(source = "iuv", target = "iuv")
    @Mapping(target = "description", constant = "-") // TODO set 'causale' from RPT
    @Mapping(target = "isPartialPayment", constant = "false")
    @Mapping(target = "retentionDate", expression = "java(null)")
    @Mapping(target = "fee", constant = "0L")
    @Mapping(target = "dueDate", expression = "java(java.time.OffsetDateTime.now().plusDays(1))")
    it.gov.pagopa.gen.wispconverter.client.gpd.model.PaymentOptionModelDto toPaymentOption(RPTContentDTO rptContentDTO);

    @Mapping(target = "hashDocument", expression = "java(new String(digitalStampDTO.getDocumentHash()))")
    @Mapping(source = "type", target = "stampType")
    @Mapping(source = "province", target = "provincialResidence")
    it.gov.pagopa.gen.wispconverter.client.gpd.model.StampDto toStamp(DigitalStampDTO digitalStampDTO);
}
