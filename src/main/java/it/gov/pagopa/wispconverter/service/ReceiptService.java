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
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.util.JaxbElementUtil;
import it.gov.pagopa.wispconverter.util.ReUtil;
import it.gov.pagopa.wispconverter.util.XmlUtil;
import jakarta.xml.bind.JAXBElement;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.xmlsoap.schemas.soap.envelope.Envelope;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReceiptService {

    private final RTMapper rtMapper;

    private final JaxbElementUtil jaxbElementUtil;

    private final ConfigCacheService configCacheService;
    private final ConverterService converterService;
    private final ReService reService;

    public void paaInviaRTKo(String payload) throws IOException {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        try {
            List<ReceiptDto> receiptDtos = List.of(mapper.readValue(payload, ReceiptDto[].class));
            //TODO: convert CPV2/SPRV2 to paaInviaRT-
            gov.telematici.pagamenti.ws.ppthead.ObjectFactory objectFactoryHead =
                    new gov.telematici.pagamenti.ws.ppthead.ObjectFactory();
            ObjectFactory objectFactory = new ObjectFactory();
            JAXBElement<CtRicevutaTelematica> rt = new it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory().createRT(generatePaaRTNegativa());
            String xmlString = jaxbElementUtil.convertToString(rt,CtRicevutaTelematica.class);

            PaaInviaRT paaInviaRT = objectFactory.createPaaInviaRT();
            paaInviaRT.setRt(Base64.getEncoder().encode(xmlString.getBytes(StandardCharsets.UTF_8)));

            ReEventDto reInternal = ReUtil.createBaseReInternal()
                    .psp("FIXME")
                    .build();
            reService.addRe(reInternal);

        } catch (JsonProcessingException e) {
            throw new AppException(AppErrorCodeMessageEnum.PARSING_INVALID_BODY);
        }
    }

    public void paaInviaRTOk(String payload) throws IOException {
        try {
            Element envelopeElement = jaxbElementUtil.convertToEnvelopeElement(payload.getBytes(StandardCharsets.UTF_8));
            Envelope envelope = jaxbElementUtil.convertToBean(envelopeElement, Envelope.class);

            PaSendRTV2Request soapBody = jaxbElementUtil.getSoapBody(envelope, PaSendRTV2Request.class);

            //TODO: convert paSendRTV2 to paaInviaRT+
//            IntestazionePPT header = generateIntestazionePPT();
            generatePaaInviaRTPositiva(soapBody);

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

        IntestazionePPT intestazionePPT = generateIntestazionePPT(paSendRTV2Request.getIdPA(),
                "",
                "",
                paSendRTV2Request.getIdBrokerPA(),
                paSendRTV2Request.getIdStation());

        it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory objectFactoryPagamenti =
                new it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory();
        CtRicevutaTelematica ctRicevutaTelematica = objectFactoryPagamenti.createCtRicevutaTelematica();

//        rtMapper.toCtRicevutaTelematica(ctRicevutaTelematica);

        //        rtService.paaInviaRT(paaInviaRT, header);
    }

    private CtRicevutaTelematica generatePaaRTNegativa() {
        ConfigDataV1Dto cache = configCacheService.getConfigData();
        Map<String, ConfigurationKeyDto> configurations = cache.getConfigurations();

        String tipoIdentificativoUnivoco =
                configurations.get("GLOBAL-istitutoAttestante.identificativoUnivocoAttestante.tipoIdentificativoUnivoco").getValue();
        String codiceIdentificativoUnivoco =
                configurations.get("GLOBAL-istitutoAttestante.identificativoUnivocoAttestante.codiceIdentificativoUnivoco").getValue();
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
        CtIdentificativoUnivoco ctIdentificativoUnivoco = objectFactory.createCtIdentificativoUnivoco();
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

        CtDominio ctDominio = objectFactory.createCtDominio();
        ctDominio.setIdentificativoDominio("");//TODO
        ctDominio.setIdentificativoStazioneRichiedente("");//TODO

        CtEnteBeneficiario ctEnteBeneficiario = objectFactory.createCtEnteBeneficiario();//TODO recuperare valori da RPT
//        ctEnteBeneficiario.setCapBeneficiario();
//        ctEnteBeneficiario.setCivicoBeneficiario();
//        ctEnteBeneficiario.setDenominazioneBeneficiario();
//        ctEnteBeneficiario.setDenomUnitOperBeneficiario();
//        ctEnteBeneficiario.setIndirizzoBeneficiario();
//        ctEnteBeneficiario.setLocalitaBeneficiario();
//        ctEnteBeneficiario.setProvinciaBeneficiario();
//        ctEnteBeneficiario.setNazioneBeneficiario();

//        CtSoggettoVersante ctSoggettoVersante = objectFactory.createCtSoggettoVersante();//TODO recuperare valori da RPT
        CtSoggettoPagatore ctSoggettoPagatore = objectFactory.createCtSoggettoPagatore();//TODO recuperare valori da RPT

        //val motivoAnnullamentoDesc = MotivoAnnullamentoEnum.description(motivoAnnullamento)

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
//        ctRicevutaTelematica.setSoggettoVersante(ctSoggettoVersante);
        ctRicevutaTelematica.setSoggettoPagatore(ctSoggettoPagatore);

        CtDatiVersamentoRT ctDatiVersamentoRT = objectFactory.createCtDatiVersamentoRT();
        ctDatiVersamentoRT.setCodiceEsitoPagamento("1");//TODO capire che valore mettere
        ctDatiVersamentoRT.setImportoTotalePagato(BigDecimal.ZERO);
        ctDatiVersamentoRT.setIdentificativoUnivocoVersamento("");//TODO recuperare valori da RPT - rpt.datiVersamento.identificativoUnivocoVersamento
        ctDatiVersamentoRT.setCodiceContestoPagamento("");//TODO recuperare valori da RPT - rpt.datiVersamento.codiceContestoPagamento

        CtDatiSingoloPagamentoRT ctDatiSingoloPagamentoRT = objectFactory.createCtDatiSingoloPagamentoRT();
        ctDatiSingoloPagamentoRT.setSingoloImportoPagato(BigDecimal.ZERO);
        ctDatiSingoloPagamentoRT.setEsitoSingoloPagamento("");//TODO cosa mettere?
        ctDatiSingoloPagamentoRT.setDataEsitoSingoloPagamento(XmlUtil.toXMLGregoirianCalendar(now));
        ctDatiSingoloPagamentoRT.setIdentificativoUnivocoRiscossione("0");
        ctDatiSingoloPagamentoRT.setCausaleVersamento("");//TODO recuperare dal versamento di RPT - dsv.causaleVersamento
        ctDatiSingoloPagamentoRT.setDatiSpecificiRiscossione("");//TODO recuperare dal versamento di RPT - dsv.datiSpecificiRiscossione
        ctDatiVersamentoRT.getDatiSingoloPagamento().add(ctDatiSingoloPagamentoRT);

        return ctRicevutaTelematica;
    }

    private IntestazionePPT generateIntestazionePPT(String idDominio, String iuv, String ccp, String idIntermediarioPa, String idStazione) {
        gov.telematici.pagamenti.ws.ppthead.ObjectFactory objectFactoryHead =
                new gov.telematici.pagamenti.ws.ppthead.ObjectFactory();

        IntestazionePPT header = objectFactoryHead.createIntestazionePPT();
        header.setIdentificativoDominio(idDominio);
        header.setIdentificativoUnivocoVersamento(iuv);
        header.setCodiceContestoPagamento(ccp);
        header.setIdentificativoIntermediarioPA(idIntermediarioPa);
        header.setIdentificativoStazioneIntermediarioPA(idStazione);
        return header;
    }


}
