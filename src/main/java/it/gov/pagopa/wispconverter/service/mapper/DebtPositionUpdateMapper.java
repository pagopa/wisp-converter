package it.gov.pagopa.wispconverter.service.mapper;

import it.gov.pagopa.gen.wispconverter.client.gpd.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DebtPositionUpdateMapper {

    PaymentPositionModelDto toPaymentPosition(PaymentPositionModelBaseResponseDto baseResponse);

    @Mapping(target = "transfer", ignore = true)
    PaymentOptionModelDto toPaymentOption(PaymentOptionModelResponseDto paymentOptionResponse);

    PaymentOptionMetadataModelDto toPaymentOptionMetadata(PaymentOptionMetadataModelResponseDto metadataResponse);
}