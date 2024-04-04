package it.gov.pagopa.wispconverter.service.mapper;

import it.gov.digitpa.schemas._2011.pagamenti.CtRicevutaTelematica;
import it.gov.digitpa.schemas._2011.pagamenti.CtSoggettoPagatore;
import it.gov.digitpa.schemas._2011.pagamenti.CtSoggettoVersante;
import it.gov.pagopa.pagopa_api.pa.pafornode.CtReceiptV2;
import it.gov.pagopa.pagopa_api.pa.pafornode.CtSubject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RTMapper {

//    @Mapping(source = "receiptId", target = "")
//    @Mapping(source = "noticeNumber", target = "")
//    @Mapping(source = "fiscalCode", target = "")
//    @Mapping(source = "outcome", target = "")
//    @Mapping(source = "creditorReferenceId", target = "")
//    @Mapping(source = "paymentAmount", target = "")
//    @Mapping(source = "description", target = "")
//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "officeName", target = "")
//    @Mapping(source = "debtor", target = "")
//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "companyName", target = "")
    void toCtRicevutaTelematica(@MappingTarget CtRicevutaTelematica ctRicevutaTelematica, CtReceiptV2 ctReceiptV2);

//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "companyName", target = "")
    void toCtSoggettoPagatore(@MappingTarget CtSoggettoPagatore ctSoggettoPagatore, CtSubject debtor);

//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "companyName", target = "")
//    @Mapping(source = "companyName", target = "")
    void toCtSoggettoVersante(@MappingTarget CtSoggettoVersante ctSoggettoVersante, CtSubject payer);

    






}
