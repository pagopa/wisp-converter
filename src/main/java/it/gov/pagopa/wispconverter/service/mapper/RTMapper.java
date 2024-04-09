package it.gov.pagopa.wispconverter.service.mapper;

import it.gov.digitpa.schemas._2011.pagamenti.*;
import it.gov.pagopa.pagopa_api.pa.pafornode.CtReceiptV2;
import it.gov.pagopa.pagopa_api.pa.pafornode.CtSubject;
import it.gov.pagopa.wispconverter.service.model.*;
import it.gov.pagopa.wispconverter.service.model.paymentrequest.PaymentRequestDTO;
import it.gov.pagopa.wispconverter.util.XmlUtil;
import org.mapstruct.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RTMapper {

    @Mapping(source = "domainId", target = "identificativoDominio")
    @Mapping(source = "stationId", target = "identificativoStazioneRichiedente")
    CtDominio toCtDominio(@MappingTarget CtDominio ctDominio, PaymentRequestDomainDTO domainDTO );

    @Mapping(source = "subjectUniqueIdentifier", target = "identificativoUnivocoBeneficiario")
    @Mapping(source = "name", target = "denominazioneBeneficiario")
    @Mapping(source = "operUnitCode", target = "codiceUnitOperBeneficiario")
    @Mapping(source = "operUnitDenom", target = "denomUnitOperBeneficiario")
    @Mapping(source = "address", target = "indirizzoBeneficiario")
    @Mapping(source = "streetNumber", target = "civicoBeneficiario")
    @Mapping(source = "postalCode", target = "capBeneficiario")
    @Mapping(source = "city", target = "localitaBeneficiario")
    @Mapping(source = "province", target = "provinciaBeneficiario")
    @Mapping(source = "nation", target = "nazioneBeneficiario")
    CtEnteBeneficiario toCtEnteBeneficiario(@MappingTarget CtEnteBeneficiario ctEnteBeneficiario, PaymentSubjectDTO payerInstitution);

    @Mapping(source = "subjectUniqueIdentifier", target = "identificativoUnivocoPagatore")
    @Mapping(source = "name", target = "anagraficaPagatore")
    @Mapping(source = "address", target = "indirizzoPagatore")
    @Mapping(source = "streetNumber", target = "civicoPagatore")
    @Mapping(source = "postalCode", target = "capPagatore")
    @Mapping(source = "city", target = "localitaPagatore")
    @Mapping(source = "province", target = "provinciaPagatore")
    @Mapping(source = "nation", target = "nazionePagatore")
    @Mapping(source = "email", target = "EMailPagatore")
    CtSoggettoPagatore toCtSoggettoPagatore(@MappingTarget CtSoggettoPagatore ctSoggettoPagatore, PaymentSubjectDTO debtor);

    @Mapping(source = "type", target = "tipoIdentificativoUnivoco")
    @Mapping(source = "code", target = "codiceIdentificativoUnivoco")
    CtIdentificativoUnivocoPersonaG toIdentificativoUnivocoBeneficiario(SubjectUniqueIdentifierDTO subjectUniqueIdentifierDTO);

    @Mapping(source = "type", target = "tipoIdentificativoUnivoco")
    @Mapping(source = "code", target = "codiceIdentificativoUnivoco")
    CtIdentificativoUnivocoPersonaFG toIdentificativoUnivocoPagatore(SubjectUniqueIdentifierDTO subjectUniqueIdentifierDTO);

    @Mapping(target = "codiceEsitoPagamento", constant = "1")
    @Mapping(target = "importoTotalePagato", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(source = "iuv", target = "identificativoUnivocoVersamento")
    @Mapping(source = "ccp", target = "codiceContestoPagamento")
    @Mapping(source = "transfer", target = "datiSingoloPagamento")
    CtDatiVersamentoRT toCtDatiVersamentoRT(@MappingTarget CtDatiVersamentoRT ctDatiVersamentoRT, TransferDataDTO transferDataDTO);

    List<CtDatiSingoloPagamentoRT> toDatiSingoloPagamentoList(List<TransferDTO> transferDTOList);

    @Mapping(target = "singoloImportoPagato", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "esitoSingoloPagamento", constant = "Annullato da WISP")//TODO cosa mettere?
    @Mapping(target = "dataEsitoSingoloPagamento", expression = "java(it.gov.pagopa.wispconverter.util.XmlUtil.toXMLGregoirianCalendar(java.time.Instant.now()))")
    @Mapping(target = "identificativoUnivocoRiscossione", constant = "0")
    @Mapping(source = "remittanceInformation", target = "causaleVersamento")
    @Mapping(source = "category", target = "datiSpecificiRiscossione")
    CtDatiSingoloPagamentoRT toCtDatiSingoloPagamentoRT(TransferDTO transferDTO);

}
