package it.gov.pagopa.wispconverter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.telematici.pagamenti.ws.ObjectFactory;
import gov.telematici.pagamenti.ws.PaaInviaRT;
import gov.telematici.pagamenti.ws.ppthead.IntestazionePPT;
import it.gov.digitpa.schemas._2011.pagamenti.*;
import it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigDataV1Dto;
import it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigurationKeyDto;
import it.gov.pagopa.pagopa_api.pa.pafornode.CtReceiptV2;
import it.gov.pagopa.pagopa_api.pa.pafornode.PaSendRTV2Request;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.service.mapper.RTMapper;
import it.gov.pagopa.wispconverter.service.model.ReceiptDto;
import it.gov.pagopa.wispconverter.util.JaxbElementUtil;
import it.gov.pagopa.wispconverter.util.XmlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.xmlsoap.schemas.soap.envelope.Envelope;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReceiptService {

    private final RTMapper rtMapper;
    private final JaxbElementUtil jaxbElementUtil;

    private final ConfigCacheService configCacheService;

    public void paaInviaRTKo(String payload) throws IOException {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        try {
            List<ReceiptDto> receiptDtos = List.of(mapper.readValue(payload, ReceiptDto[].class));
            //TODO: convert CPV2/SPRV2 to paaInviaRT-

            paaInviaRTNegativa();
            return;
        } catch (JsonProcessingException e) {
            throw new AppException(AppErrorCodeMessageEnum.PARSING_INVALID_BODY);
        }
    }

    public void paaInviaRTOk(String payload) throws IOException {
        try {
            Element envelopeElement = jaxbElementUtil.convertToEnvelopeElement(payload.getBytes(StandardCharsets.UTF_8));
            Envelope envelope = jaxbElementUtil.convertToBean(envelopeElement, Envelope.class);

            IntestazionePPT soapHeader = jaxbElementUtil.getSoapHeader(envelope, IntestazionePPT.class);
            PaSendRTV2Request soapBody = jaxbElementUtil.getSoapBody(envelope, PaSendRTV2Request.class);
            String idDominio = soapHeader.getIdentificativoDominio();

            //TODO: convert paSendRTV2 to paaInviaRT-
            IntestazionePPT header = generateIntestazionePPT();
            generatePaaInviaRTPositiva(soapBody);

            return;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(AppErrorCodeMessageEnum.GENERIC_ERROR);
        }
    }

    private void generatePaaInviaRTPositiva(PaSendRTV2Request paSendRTV2Request) {
        CtReceiptV2 ctReceiptV2 = paSendRTV2Request.getReceipt();

        gov.telematici.pagamenti.ws.ppthead.ObjectFactory objectFactoryHead =
                new gov.telematici.pagamenti.ws.ppthead.ObjectFactory();
        ObjectFactory objectFactory = new ObjectFactory();
        PaaInviaRT paaInviaRT = objectFactory.createPaaInviaRT();

        it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory objectFactoryPagamenti =
                new it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory();
        CtRicevutaTelematica ctRicevutaTelematica = objectFactoryPagamenti.createCtRicevutaTelematica();

//        rtMapper.toCtRicevutaTelematica(ctRicevutaTelematica);

        //        rtService.paaInviaRT(paaInviaRT, header);
    }

    private void paaInviaRTNegativa() {
        ConfigDataV1Dto cache = configCacheService.getCache();

        Map<String, ConfigurationKeyDto> configurations = cache.getConfigurations();

        String tipoIdentificativoUnivoco =
                configurations.get("GLOBAL-istitutoAttestante.identificativoUnivocoAttestante.tipoIdentificativoUnivoco").getValue();
        String codiceIdentificativoUnivoco = configurations.get("GLOBAL-istitutoAttestante.identificativoUnivocoAttestante.codiceIdentificativoUnivoco").getValue();
        String denominazioneAttestante = configurations.get("GLOBAL-istitutoAttestante.denominazioneAttestante").getValue();
        String codiceUnitOperAttestante = configurations.get("GLOBAL-istitutoAttestante.codiceUnitOperAttestante").getValue();
        String denomUnitOperAttestante = configurations.get("GLOBAL-istitutoAttestante.denomUnitOperAttestante").getValue();
        String indirizzoAttestante = configurations.get("GLOBAL-istitutoAttestante.indirizzoAttestante").getValue();
        String civicoAttestante = configurations.get("GLOBAL-istitutoAttestante.civicoAttestante").getValue();
        String capAttestante = configurations.get("GLOBAL-istitutoAttestante.capAttestante").getValue();
        String localitaAttestante = configurations.get("GLOBAL-istitutoAttestante.localitaAttestante").getValue();
        String provinciaAttestante = configurations.get("GLOBAL-istitutoAttestante.provinciaAttestante").getValue();
        String nazioneAttestante = configurations.get("GLOBAL-istitutoAttestante.nazioneAttestante").getValue();

        it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory objectFactory = new it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory();
        CtIstitutoAttestante ctIstitutoAttestante = objectFactory.createCtIstitutoAttestante();
        ctIstitutoAttestante.setDenominazioneAttestante("");
        ctIstitutoAttestante.setCodiceUnitOperAttestante("");
        ctIstitutoAttestante.setDenomUnitOperAttestante("");
        ctIstitutoAttestante.setIndirizzoAttestante("");

        CtDominio ctDominio = objectFactory.createCtDominio();
        ctDominio.setIdentificativoDominio("");//TODO
        ctDominio.setIdentificativoStazioneRichiedente("");//TODO

        CtEnteBeneficiario ctEnteBeneficiario = objectFactory.createCtEnteBeneficiario();//TODO recuperare valori da RPT

        CtSoggettoVersante ctSoggettoVersante = objectFactory.createCtSoggettoVersante();//TODO recuperare valori da RPT
        CtSoggettoPagatore ctSoggettoPagatore = objectFactory.createCtSoggettoPagatore();//TODO recuperare valori da RPT

        //TODO: recuperare da cache i valori dalla tabella CONFIGURATION_KEYS
//        val tipoIdentificativoUnivoco = DDataChecks.getConfigurationKeys(ddataMap, "istitutoAttestante.identificativoUnivocoAttestante.tipoIdentificativoUnivoco")
//        val codiceIdentificativoUnivoco = DDataChecks.getConfigurationKeys(ddataMap, "istitutoAttestante.identificativoUnivocoAttestante.codiceIdentificativoUnivoco")
//        val denominazioneAttestante = DDataChecks.getConfigurationKeys(ddataMap, "istitutoAttestante.denominazioneAttestante")
//        val codiceUnitOperAttestante = DDataChecks.getConfigurationKeys(ddataMap, "istitutoAttestante.codiceUnitOperAttestante")
//        val denomUnitOperAttestante = DDataChecks.getConfigurationKeys(ddataMap, "istitutoAttestante.denomUnitOperAttestante")
//        val indirizzoAttestante = DDataChecks.getConfigurationKeys(ddataMap, "istitutoAttestante.indirizzoAttestante")
//        val civicoAttestante = DDataChecks.getConfigurationKeys(ddataMap, "istitutoAttestante.civicoAttestante")
//        val capAttestante = DDataChecks.getConfigurationKeys(ddataMap, "istitutoAttestante.capAttestante")
//        val localitaAttestante = DDataChecks.getConfigurationKeys(ddataMap, "istitutoAttestante.localitaAttestante")
//        val provinciaAttestante = DDataChecks.getConfigurationKeys(ddataMap, "istitutoAttestante.provinciaAttestante")
//        val nazioneAttestante = DDataChecks.getConfigurationKeys(ddataMap, "istitutoAttestante.nazioneAttestante")

        //                                        val motivoAnnullamentoDesc = MotivoAnnullamentoEnum.description(motivoAnnullamento)


        CtRicevutaTelematica ctRicevutaTelematica = objectFactory.createCtRicevutaTelematica();

        ctRicevutaTelematica.setVersioneOggetto("6.2.0");
        ctRicevutaTelematica.setDominio(ctDominio);
        ctRicevutaTelematica.setIdentificativoMessaggioRicevuta(UUID.randomUUID().toString().replaceAll("-", ""));

        Instant now = Instant.now();
        ctRicevutaTelematica.setDataOraMessaggioRicevuta(XmlUtil.toXMLGregoirianCalendar(now));
        ctRicevutaTelematica.setIdentificativoMessaggioRicevuta("");//TODO
        ctRicevutaTelematica.setRiferimentoDataRichiesta(null);//TODO
        ctRicevutaTelematica.setIstitutoAttestante(ctIstitutoAttestante);

        ctRicevutaTelematica.setEnteBeneficiario(ctEnteBeneficiario);
        ctRicevutaTelematica.setSoggettoVersante(ctSoggettoVersante);
        ctRicevutaTelematica.setSoggettoPagatore(ctSoggettoPagatore);

        CtDatiVersamentoRT ctDatiVersamentoRT = objectFactory.createCtDatiVersamentoRT();
        ctDatiVersamentoRT.setCodiceEsitoPagamento("1");//TODO capire che valore mettere
        ctDatiVersamentoRT.setImportoTotalePagato(BigDecimal.ZERO);
        ctDatiVersamentoRT.setIdentificativoUnivocoVersamento("");//TODO recuperare valori da RPT
        ctDatiVersamentoRT.setCodiceContestoPagamento("");//TODO recuperare valori da RPT

        CtDatiSingoloPagamentoRT ctDatiSingoloPagamentoRT = objectFactory.createCtDatiSingoloPagamentoRT();
        ctDatiSingoloPagamentoRT.setSingoloImportoPagato(BigDecimal.ZERO);
        ctDatiSingoloPagamentoRT.setEsitoSingoloPagamento("");//TODO cosa mettere?
        ctDatiSingoloPagamentoRT.setDataEsitoSingoloPagamento(XmlUtil.toXMLGregoirianCalendar(now));
        ctDatiSingoloPagamentoRT.setIdentificativoUnivocoRiscossione("0");
        ctDatiSingoloPagamentoRT.setCausaleVersamento("");//TODO recuperare dal versamento di RPT
        ctDatiSingoloPagamentoRT.setDatiSpecificiRiscossione("");//TODO recuperare dal versamento di RPT
        ctDatiVersamentoRT.getDatiSingoloPagamento().add(ctDatiSingoloPagamentoRT);

//                                        val body = CtRicevutaTelematica(
//                                        "6.2.0",
//                                        rpt.dominio,
//                                        UUID.randomUUID().toString.replaceAll("-", ""), //check
//                                        XmlUtil.StringXMLGregorianCalendarDate.format(now, XsdDatePattern.DATE_TIME), //check
//                                        rpt.identificativoMessaggioRichiesta,
//                                        XmlUtil.StringXMLGregorianCalendarDate.format(rpt.dataOraMessaggioRichiesta.toGregorianCalendar.toZonedDateTime.toLocalDateTime, XsdDatePattern.DATE),
//                                        istitutoAttestante,
//                                        rpt.enteBeneficiario,
//                                        rpt.soggettoVersante,
//                                        rpt.soggettoPagatore,
//                                        CtDatiVersamentoRT(
//                                                codiceEsitoPagamento,
//                                                BigDecimal2(BigDecimal(0)),
//                                                rpt.datiVersamento.identificativoUnivocoVersamento,
//                                                rpt.datiVersamento.codiceContestoPagamento,
//                                                rpt.datiVersamento.datiSingoloVersamento.map(dsv => {
//                                                        CtDatiSingoloPagamentoRT(
//                                                                BigDecimal2(BigDecimal(0)),
//                                                                Some(motivoAnnullamentoDesc),
//                                                                XmlUtil.StringXMLGregorianCalendarDate.format(now, XsdDatePattern.DATE),
//                                                                "0",
//                                                                dsv.causaleVersamento,
//                                                                dsv.datiSpecificiRiscossione,
//                                                                None,
//                                                                None
//                                                        )
//                                                })
//                                )
    }

    private IntestazionePPT generateIntestazionePPT() {
        gov.telematici.pagamenti.ws.ppthead.ObjectFactory objectFactoryHead =
                new gov.telematici.pagamenti.ws.ppthead.ObjectFactory();

        IntestazionePPT header = objectFactoryHead.createIntestazionePPT();
//        header.setIdentificativoStazioneIntermediarioPA(paSendRTV2Request.getIdStation());
//        header.setCodiceContestoPagamento(ctReceiptV2.getCreditorReferenceId());
//        header.setIdentificativoDominio(ctReceiptV2.getFiscalCode());
//        header.setIdentificativoUnivocoVersamento(ctReceiptV2.getNoticeNumber());
//        header.setIdentificativoIntermediarioPA(paSendRTV2Request.getIdBrokerPA());
        return header;
    }


}
