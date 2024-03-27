package it.gov.pagopa.wispconverter.service.mapper;

import it.gov.digitpa.schemas._2011.pagamenti.*;
import it.gov.pagopa.wispconverter.service.model.*;
import it.gov.pagopa.wispconverter.service.model.paymentrequest.PaymentRequestDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RPTMapper {

    @Mapping(source = "versioneOggetto", target = "version")
    @Mapping(source = "dominio", target = "domain")
    @Mapping(source = "identificativoMessaggioRichiesta", target = "messageRequestId")
    @Mapping(source = "dataOraMessaggioRichiesta", target = "messageRequestDatetime")
    @Mapping(source = "autenticazioneSoggetto", target = "subjectAuthentication")
    @Mapping(source = "soggettoVersante", target = "debtor")
    @Mapping(source = "soggettoPagatore", target = "payer")
    @Mapping(source = "enteBeneficiario", target = "payeeInstitution")
    @Mapping(source = "datiVersamento", target = "transferData")
    PaymentRequestDTO toPaymentRequestDTO(CtRichiestaPagamentoTelematico rpt);

    @Mapping(source = "identificativoDominio", target = "domainId")
    @Mapping(source = "identificativoStazioneRichiedente", target = "stationId")
    PaymentRequestDomainDTO toPaymentRequestDomainDTO(CtDominio dominio);

    @Mapping(source = "identificativoUnivocoPagatore", target = "subjectUniqueIdentifier")
    @Mapping(source = "anagraficaPagatore", target = "name")
    @Mapping(source = "indirizzoPagatore", target = "address")
    @Mapping(source = "civicoPagatore", target = "streetNumber")
    @Mapping(source = "capPagatore", target = "postalCode")
    @Mapping(source = "localitaPagatore", target = "city")
    @Mapping(source = "provinciaPagatore", target = "province")
    @Mapping(source = "nazionePagatore", target = "nation")
    @Mapping(source = "EMailPagatore", target = "email")
    PaymentSubjectDTO toPayerPaymentSubjectDTO(CtSoggettoPagatore soggettoPagatore);

    @Mapping(source = "identificativoUnivocoVersante", target = "subjectUniqueIdentifier")
    @Mapping(source = "anagraficaVersante", target = "name")
    @Mapping(source = "indirizzoVersante", target = "address")
    @Mapping(source = "civicoVersante", target = "streetNumber")
    @Mapping(source = "capVersante", target = "postalCode")
    @Mapping(source = "localitaVersante", target = "city")
    @Mapping(source = "provinciaVersante", target = "province")
    @Mapping(source = "nazioneVersante", target = "nation")
    @Mapping(source = "EMailVersante", target = "email")
    PaymentSubjectDTO toDebtorPaymentSubjectDTO(CtSoggettoVersante soggettoVersante);

    @Mapping(source = "identificativoUnivocoBeneficiario", target = "subjectUniqueIdentifier")
    @Mapping(source = "denominazioneBeneficiario", target = "name")
    @Mapping(source = "codiceUnitOperBeneficiario", target = "operUnitCode")
    @Mapping(source = "denomUnitOperBeneficiario", target = "operUnitDenom")
    @Mapping(source = "indirizzoBeneficiario", target = "address")
    @Mapping(source = "civicoBeneficiario", target = "streetNumber")
    @Mapping(source = "capBeneficiario", target = "postalCode")
    @Mapping(source = "localitaBeneficiario", target = "city")
    @Mapping(source = "provinciaBeneficiario", target = "province")
    @Mapping(source = "nazioneBeneficiario", target = "nation")
    PaymentSubjectDTO toPaymentPayeeInstitutionDTO(CtEnteBeneficiario enteBeneficiario);

    @Mapping(source = "tipoIdentificativoUnivoco", target = "type")
    @Mapping(source = "codiceIdentificativoUnivoco", target = "code")
    SubjectUniqueIdentifierDTO toIdentificativoUnivocoPersonaFGDTO(CtIdentificativoUnivocoPersonaFG identificativoUnivoco);

    @Mapping(source = "tipoIdentificativoUnivoco", target = "type")
    @Mapping(source = "codiceIdentificativoUnivoco", target = "code")
    SubjectUniqueIdentifierDTO toIdentificativoUnivocoPersonaGDTO(CtIdentificativoUnivocoPersonaG identificativoUnivoco);


    @Mapping(source = "dataEsecuzionePagamento", target = "paymentDate")
    @Mapping(source = "importoTotaleDaVersare", target = "totalAmount")
    @Mapping(source = "tipoVersamento", target = "type")
    @Mapping(source = "identificativoUnivocoVersamento", target = "iuv")
    @Mapping(source = "codiceContestoPagamento", target = "ccp")
    @Mapping(source = "ibanAddebito", target = "debitIban")
    @Mapping(source = "bicAddebito", target = "debitBic")
    @Mapping(source = "firmaRicevuta", target = "rtSignature")
    @Mapping(source = "datiSingoloVersamento", target = "transfer")
    TransferDataDTO toTransferDataDTO(CtDatiVersamentoRPT datiVersamentoRPT);


    @Mapping(source = "importoSingoloVersamento", target = "amount")
    @Mapping(source = "commissioneCaricoPA", target = "fee")
    @Mapping(source = "ibanAccredito", target = "creditIban")
    @Mapping(source = "bicAccredito", target = "creditBic")
    @Mapping(source = "ibanAppoggio", target = "supportIban")
    @Mapping(source = "bicAppoggio", target = "supportBic")
    @Mapping(source = "credenzialiPagatore", target = "payerCredentials")
    @Mapping(source = "causaleVersamento", target = "remittanceInformation")
    @Mapping(source = "datiSpecificiRiscossione", target = "category")
    @Mapping(source = "datiMarcaBolloDigitale", target = "digitalStamp")
    TransferDTO toTransferDTO(CtDatiSingoloVersamentoRPT datiSingoloVersamentoRPT);

    @Mapping(source = "tipoBollo", target = "type")
    @Mapping(source = "hashDocumento", target = "documentHash")
    @Mapping(source = "provinciaResidenza", target = "province")
    DigitalStampDTO toDigitalStampDTO(CtDatiMarcaBolloDigitale datiMarcaBolloDigitale);
}
