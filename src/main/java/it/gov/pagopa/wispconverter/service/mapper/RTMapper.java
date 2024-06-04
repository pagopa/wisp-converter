package it.gov.pagopa.wispconverter.service.mapper;

import gov.telematici.pagamenti.ws.pafornode.CtReceiptV2;
import gov.telematici.pagamenti.ws.pafornode.CtSubject;
import gov.telematici.pagamenti.ws.pafornode.CtTransferPAReceiptV2;
import gov.telematici.pagamenti.ws.pafornode.PaSendRTV2Request;
import it.gov.digitpa.schemas._2011.pagamenti.*;
import it.gov.pagopa.pagopa_api.xsd.common_types.v1_0.CtMapEntry;
import it.gov.pagopa.wispconverter.service.model.*;
import it.gov.pagopa.wispconverter.service.model.paymentrequest.PaymentRequestDTO;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class RTMapper {

    @Value("${wisp-converter.rtMapper.ctRicevutaTelematica.versioneOggetto}")
    String versioneOggetto;

    public void toCtIstitutoAttestante(@MappingTarget CtIstitutoAttestante ctIstitutoAttestante,
                                       CtIdentificativoUnivoco ctIdentificativoUnivoco,
                                       Map<String, it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigurationKeyDto> configurations) {
        String tipoIdentificativoUnivoco = CommonUtility.getConfigKeyValueCache(configurations, "GLOBAL-istitutoAttestante.identificativoUnivocoAttestante.tipoIdentificativoUnivoco");
        String codiceIdentificativoUnivoco = CommonUtility.getConfigKeyValueCache(configurations, "GLOBAL-istitutoAttestante.identificativoUnivocoAttestante.codiceIdentificativoUnivoco");
        String denominazioneAttestante = CommonUtility.getConfigKeyValueCache(configurations, "GLOBAL-istitutoAttestante.denominazioneAttestante");
        String codiceUnitOperAttestante = CommonUtility.getConfigKeyValueCache(configurations, "GLOBAL-istitutoAttestante.codiceUnitOperAttestante");
        String denomUnitOperAttestante = CommonUtility.getConfigKeyValueCache(configurations, "GLOBAL-istitutoAttestante.denomUnitOperAttestante");
        String indirizzoAttestante = CommonUtility.getConfigKeyValueCache(configurations, "GLOBAL-istitutoAttestante.indirizzoAttestante");
        String civicoAttestante = CommonUtility.getConfigKeyValueCache(configurations, "GLOBAL-istitutoAttestante.civicoAttestante");
        String capAttestante = CommonUtility.getConfigKeyValueCache(configurations, "GLOBAL-istitutoAttestante.capAttestante");
        String localitaAttestante = CommonUtility.getConfigKeyValueCache(configurations, "GLOBAL-istitutoAttestante.localitaAttestante");
        String provinciaAttestante = CommonUtility.getConfigKeyValueCache(configurations, "GLOBAL-istitutoAttestante.provinciaAttestante");
        String nazioneAttestante = CommonUtility.getConfigKeyValueCache(configurations, "GLOBAL-istitutoAttestante.nazioneAttestante");

        ctIdentificativoUnivoco.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivoco.fromValue(tipoIdentificativoUnivoco));
        ctIdentificativoUnivoco.setCodiceIdentificativoUnivoco(codiceIdentificativoUnivoco);
        ctIstitutoAttestante.setIdentificativoUnivocoAttestante(ctIdentificativoUnivoco);
        ctIstitutoAttestante.setDenominazioneAttestante(denominazioneAttestante);
        ctIstitutoAttestante.setCodiceUnitOperAttestante(codiceUnitOperAttestante);
        ctIstitutoAttestante.setDenomUnitOperAttestante(denomUnitOperAttestante);
        ctIstitutoAttestante.setIndirizzoAttestante(indirizzoAttestante);
        ctIstitutoAttestante.setCivicoAttestante(civicoAttestante);
        ctIstitutoAttestante.setCapAttestante(capAttestante);
        ctIstitutoAttestante.setLocalitaAttestante(localitaAttestante);
        ctIstitutoAttestante.setProvinciaAttestante(provinciaAttestante);
        ctIstitutoAttestante.setNazioneAttestante(nazioneAttestante);
    }

    @Mapping(target = "identificativoUnivocoAttestante.tipoIdentificativoUnivoco", expression = "java(it.gov.digitpa.schemas._2011.pagamenti.StTipoIdentificativoUnivoco.G)")
    @Mapping(source = "receipt.fiscalCode", target = "identificativoUnivocoAttestante.codiceIdentificativoUnivoco")
    @Mapping(source = "receipt.PSPCompanyName", target = "denominazioneAttestante")
    public abstract void toCtIstitutoAttestante(@MappingTarget CtIstitutoAttestante ctIstitutoAttestante, PaSendRTV2Request paSendRTV2Request);

    @Mapping(target = "versioneOggetto", expression = "java(this.versioneOggetto)")
    @Mapping(target = "identificativoMessaggioRicevuta", expression = "java(java.util.UUID.randomUUID().toString().replaceAll(\"-\", \"\"))")
    @Mapping(target = "dataOraMessaggioRicevuta", expression = "java(it.gov.pagopa.wispconverter.util.XmlUtil.toXMLGregoirianCalendar(instant))")
    @Mapping(source = "rpt.messageRequestId", target = "riferimentoMessaggioRichiesta")
    @Mapping(source = "rpt.messageRequestDatetime", target = "riferimentoDataRichiesta")
    public abstract void toCtRicevutaTelematicaNegativa(@MappingTarget CtRicevutaTelematica ctRicevutaTelematica, PaymentRequestDTO rpt, Instant instant);

    @Mapping(target = "versioneOggetto", expression = "java(this.versioneOggetto)")
    @Mapping(target = "identificativoMessaggioRicevuta", expression = "java(java.util.UUID.randomUUID().toString().replaceAll(\"-\", \"\"))")
    @Mapping(source = "paSendRTV2Request.receipt.paymentDateTime", target = "dataOraMessaggioRicevuta")
    @Mapping(source = "rpt.messageRequestId", target = "riferimentoMessaggioRichiesta")
    @Mapping(source = "paSendRTV2Request.receipt.applicationDate", target = "riferimentoDataRichiesta")
    public abstract void toCtRicevutaTelematicaPositiva(@MappingTarget CtRicevutaTelematica ctRicevutaTelematica, PaymentRequestDTO rpt, PaSendRTV2Request paSendRTV2Request);

    @Mapping(source = "domainId", target = "identificativoDominio")
    @Mapping(source = "stationId", target = "identificativoStazioneRichiedente")
    public abstract void toCtDominio(@MappingTarget CtDominio ctDominio, PaymentRequestDomainDTO domainDTO);

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
    public abstract void toCtEnteBeneficiario(@MappingTarget CtEnteBeneficiario ctEnteBeneficiario, PaymentSubjectDTO payeeInstitution);

    @Mapping(source = "subjectUniqueIdentifier", target = "identificativoUnivocoPagatore")
    @Mapping(source = "name", target = "anagraficaPagatore")
    @Mapping(source = "address", target = "indirizzoPagatore")
    @Mapping(source = "streetNumber", target = "civicoPagatore")
    @Mapping(source = "postalCode", target = "capPagatore")
    @Mapping(source = "city", target = "localitaPagatore")
    @Mapping(source = "province", target = "provinciaPagatore")
    @Mapping(source = "nation", target = "nazionePagatore")
    @Mapping(source = "email", target = "EMailPagatore")
    public abstract void toCtSoggettoPagatore(@MappingTarget CtSoggettoPagatore ctSoggettoPagatore, PaymentSubjectDTO debtor);

    @Mapping(source = "uniqueIdentifier", target = "identificativoUnivocoPagatore")
    @Mapping(source = "fullName", target = "anagraficaPagatore")
    @Mapping(source = "streetName", target = "indirizzoPagatore")
    @Mapping(source = "civicNumber", target = "civicoPagatore")
    @Mapping(source = "postalCode", target = "capPagatore")
    @Mapping(source = "city", target = "localitaPagatore")
    @Mapping(source = "stateProvinceRegion", target = "provinciaPagatore")
    @Mapping(source = "country", target = "nazionePagatore")
    @Mapping(source = "EMail", target = "EMailPagatore")
    public abstract void toCtSoggettoPagatore(@MappingTarget CtSoggettoPagatore ctSoggettoPagatore, CtSubject debtor);

    @Mapping(source = "type", target = "tipoIdentificativoUnivoco")
    @Mapping(source = "code", target = "codiceIdentificativoUnivoco")
    public abstract CtIdentificativoUnivocoPersonaG toIdentificativoUnivocoBeneficiario(SubjectUniqueIdentifierDTO subjectUniqueIdentifierDTO);

    @Mapping(source = "type", target = "tipoIdentificativoUnivoco")
    @Mapping(source = "code", target = "codiceIdentificativoUnivoco")
    public abstract CtIdentificativoUnivocoPersonaFG toIdentificativoUnivocoPagatore(SubjectUniqueIdentifierDTO subjectUniqueIdentifierDTO);

    @Mapping(target = "codiceEsitoPagamento", constant = "1")
    @Mapping(target = "importoTotalePagato", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(source = "transferDataDTO.iuv", target = "identificativoUnivocoVersamento")
    @Mapping(source = "transferDataDTO.ccp", target = "codiceContestoPagamento")
    @Mapping(target = "datiSingoloPagamento", expression = "java(toCtDatiSingoloPagamentoRTList(transferDataDTO.getTransfer(), instant))")
    public abstract void toCtDatiVersamentoRT(@MappingTarget CtDatiVersamentoRT ctDatiVersamentoRT, TransferDataDTO transferDataDTO, @Context Instant instant);

    @Mapping(target = "codiceEsitoPagamento", constant = "0")
    @Mapping(source = "ctReceiptV2.paymentAmount", target = "importoTotalePagato")
    @Mapping(source = "ctReceiptV2.creditorReferenceId", target = "identificativoUnivocoVersamento")
    @Mapping(source = "transferDataDTO.ccp", target = "codiceContestoPagamento")
    @Mapping(target = "datiSingoloPagamento", expression = "java(toCtDatiSingoloPagamentoRTList(ctReceiptV2))")
    public abstract void toCtDatiVersamentoRT(@MappingTarget CtDatiVersamentoRT ctDatiVersamentoRT, TransferDataDTO transferDataDTO, CtReceiptV2 ctReceiptV2);

    public List<CtDatiSingoloPagamentoRT> toCtDatiSingoloPagamentoRTList(List<TransferDTO> transferDTOList, Instant instant) {
        List<CtDatiSingoloPagamentoRT> ctDatiSingoloPagamentoRTList = new ArrayList<>();
        transferDTOList.forEach(transferDTO -> {
            ctDatiSingoloPagamentoRTList.add(toCtDatiSingoloPagamentoRT(transferDTO, instant));
        });
        return ctDatiSingoloPagamentoRTList;
    }

    public List<CtDatiSingoloPagamentoRT> toCtDatiSingoloPagamentoRTList(CtReceiptV2 ctReceiptV2) {
        List<CtDatiSingoloPagamentoRT> ctDatiSingoloPagamentoRTList = new ArrayList<>();
        ctReceiptV2.getTransferList().getTransfer().forEach(ctTransferPAReceiptV2 -> {
            ctDatiSingoloPagamentoRTList.add(toCtDatiSingoloPagamentoRT(ctTransferPAReceiptV2, ctReceiptV2));
        });
        return ctDatiSingoloPagamentoRTList;
    }

    @Mapping(target = "singoloImportoPagato", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "esitoSingoloPagamento", constant = "Annullato da WISP")//TODO cosa mettere?
    @Mapping(target = "dataEsitoSingoloPagamento", expression = "java(it.gov.pagopa.wispconverter.util.XmlUtil.toXMLGregoirianCalendar(instant))")
    @Mapping(target = "identificativoUnivocoRiscossione", constant = "0")
    @Mapping(source = "transferDTO.remittanceInformation", target = "causaleVersamento")
    @Mapping(source = "transferDTO.category", target = "datiSpecificiRiscossione")
    public abstract CtDatiSingoloPagamentoRT toCtDatiSingoloPagamentoRT(TransferDTO transferDTO, Instant instant);

    @Mapping(source = "ctTransferPAReceiptV2.transferAmount", target = "singoloImportoPagato")
    @Mapping(target = "esitoSingoloPagamento", constant = "ESEGUITO")
    @Mapping(source = "ctReceiptV2.transferDate", target = "dataEsitoSingoloPagamento")
    @Mapping(source = "ctReceiptV2.receiptId", target = "identificativoUnivocoRiscossione")
    @Mapping(source = "ctTransferPAReceiptV2.remittanceInformation", target = "causaleVersamento")
    @Mapping(target = "ctReceiptV2.datiSpecificiRiscossione", qualifiedByName = "java(extractMetadata(ctReceiptV2.getMetadata().getMapEntry()))")
    @Mapping(source = "ctReceiptV2.fee", target = "commissioniApplicatePSP")
    public abstract CtDatiSingoloPagamentoRT toCtDatiSingoloPagamentoRT(CtTransferPAReceiptV2 ctTransferPAReceiptV2, CtReceiptV2 ctReceiptV2);

    @Named("extractMetadata")
    private String extractMetadata(List<CtMapEntry> ctMapEntries) {
        return ctMapEntries
                .stream()
                .filter(v -> v.getKey().equals("DatiSpecificiRiscossione"))
                .map(CtMapEntry::getValue).findFirst().orElse(null);
    }
}
