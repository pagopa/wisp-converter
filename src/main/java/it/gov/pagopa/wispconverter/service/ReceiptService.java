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
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.service.mapper.RTMapper;
import it.gov.pagopa.wispconverter.service.model.CommonRPTFieldsDTO;
import it.gov.pagopa.wispconverter.service.model.RPTContentDTO;
import it.gov.pagopa.wispconverter.service.model.ReceiptDto;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.util.JaxbElementUtil;
import it.gov.pagopa.wispconverter.util.ReUtil;
import it.gov.pagopa.wispconverter.util.XmlUtil;
import jakarta.xml.bind.JAXBElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.xmlsoap.schemas.soap.envelope.Envelope;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
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
    private final ConverterService converterService;
    private final RptCosmosService rptCosmosService;
    private final RPTExtractorService rptExtractorService;
    private final ReService reService;

    public void paaInviaRTKo(String payload) throws IOException {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        try {
            List<ReceiptDto> receiptDtos = List.of(mapper.readValue(payload, ReceiptDto[].class));
            ObjectFactory objectFactory = new ObjectFactory();

            receiptDtos.stream().forEach(receipt -> {
                RPTRequestEntity rptRequestEntity = rptCosmosService.getRPTRequestEntity("intPaLorenz_75fa058f-d1b5-4c7e-865e-a220091d3954");

                CommonRPTFieldsDTO commonRPTFieldsDTO = this.rptExtractorService.extractRPTContentDTOs(rptRequestEntity.getPrimitive(), rptRequestEntity.getPayload());

                IntestazionePPT intestazionePPT = generateIntestazionePPT(
                        receipt.getIdentificativoDominio(),
                        receipt.getIdentificativoUnivocoVersamento(),
                        receipt.getPaymentToken(),
                        commonRPTFieldsDTO.getCreditorInstitutionBrokerId(),
                        commonRPTFieldsDTO.getStationId());
                JAXBElement<CtRicevutaTelematica> rt = new it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory().createRT(generatePaaRTNegativa(commonRPTFieldsDTO.getRpts()));
                String xmlString = jaxbElementUtil.convertToString(rt,CtRicevutaTelematica.class);

                PaaInviaRT paaInviaRT = objectFactory.createPaaInviaRT();
                paaInviaRT.setRt(Base64.getEncoder().encode(xmlString.getBytes(StandardCharsets.UTF_8)));

                ReEventDto reInternal = ReUtil.createBaseReInternal()
                        .status("RT_GENERATA_NODO")
                        .build();
                reService.addRe(reInternal);
            });

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
            generatePaaRTPositiva(soapBody);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(AppErrorCodeMessageEnum.GENERIC_ERROR);
        }
    }

    private void generatePaaRTPositiva(PaSendRTV2Request paSendRTV2Request) {
        CtReceiptV2 ctReceiptV2 = paSendRTV2Request.getReceipt();


        ObjectFactory objectFactory = new ObjectFactory();
        PaaInviaRT paaInviaRT = objectFactory.createPaaInviaRT();



        it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory objectFactoryPagamenti =
                new it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory();
        CtRicevutaTelematica ctRicevutaTelematica = objectFactoryPagamenti.createCtRicevutaTelematica();

//        rtMapper.toCtRicevutaTelematica(ctRicevutaTelematica);

        //        rtService.paaInviaRT(paaInviaRT, header);
    }

    private CtRicevutaTelematica generatePaaRTNegativa(List<RPTContentDTO> rpts) {
        Instant now = Instant.now();
        ConfigDataV1Dto cache = configCacheService.getConfigData();
        Map<String, ConfigurationKeyDto> configurations = cache.getConfigurations();

        it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory objectFactory = new it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory();
        CtRicevutaTelematica ctRicevutaTelematica = objectFactory.createCtRicevutaTelematica();

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

        rpts.stream().forEach(rpt -> {
            CtDominio ctDominio = objectFactory.createCtDominio();
            ctDominio.setIdentificativoDominio(rpt.getRpt().getDomain().getDomainId());
            ctDominio.setIdentificativoStazioneRichiedente(rpt.getRpt().getDomain().getStationId());

            CtEnteBeneficiario ctEnteBeneficiario = objectFactory.createCtEnteBeneficiario();
            ctEnteBeneficiario.setCapBeneficiario(rpt.getRpt().getPayerInstitution().getPostalCode());
            ctEnteBeneficiario.setCivicoBeneficiario(rpt.getRpt().getPayerInstitution().getStreetNumber());
            ctEnteBeneficiario.setDenominazioneBeneficiario(rpt.getRpt().getPayerInstitution().getName());
            ctEnteBeneficiario.setDenomUnitOperBeneficiario(rpt.getRpt().getPayerInstitution().getOperUnitDenom());
            ctEnteBeneficiario.setIndirizzoBeneficiario(rpt.getRpt().getPayerInstitution().getAddress());
            ctEnteBeneficiario.setLocalitaBeneficiario(rpt.getRpt().getPayerInstitution().getPostalCode());
            ctEnteBeneficiario.setProvinciaBeneficiario(rpt.getRpt().getPayerInstitution().getProvince());
            ctEnteBeneficiario.setNazioneBeneficiario(rpt.getRpt().getPayerInstitution().getNation());

            CtSoggettoPagatore ctSoggettoPagatore = objectFactory.createCtSoggettoPagatore();
            CtIdentificativoUnivocoPersonaFG iuSoggettoPagatoreFG = objectFactory.createCtIdentificativoUnivocoPersonaFG();
            iuSoggettoPagatoreFG.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.fromValue(rpt.getRpt().getPayer().getSubjectUniqueIdentifier().getType()));
            iuSoggettoPagatoreFG.setCodiceIdentificativoUnivoco(rpt.getRpt().getPayer().getSubjectUniqueIdentifier().getCode());
            ctSoggettoPagatore.setIdentificativoUnivocoPagatore(iuSoggettoPagatoreFG);
            ctSoggettoPagatore.setAnagraficaPagatore(rpt.getRpt().getPayer().getName());
            ctSoggettoPagatore.setIndirizzoPagatore(rpt.getRpt().getPayer().getAddress());
            ctSoggettoPagatore.setCivicoPagatore(rpt.getRpt().getPayer().getStreetNumber());
            ctSoggettoPagatore.setLocalitaPagatore(rpt.getRpt().getPayer().getCity());
            ctSoggettoPagatore.setCapPagatore(rpt.getRpt().getPayer().getPostalCode());
            ctSoggettoPagatore.setProvinciaPagatore(rpt.getRpt().getPayer().getProvince());
            ctSoggettoPagatore.setNazionePagatore(rpt.getRpt().getPayer().getNation());
            ctSoggettoPagatore.setEMailPagatore(rpt.getRpt().getPayer().getEmail());

            ctRicevutaTelematica.setVersioneOggetto("6.2.0");
            ctRicevutaTelematica.setDominio(ctDominio);

            ctRicevutaTelematica.setIdentificativoMessaggioRicevuta(UUID.randomUUID().toString().replaceAll("-", ""));
            ctRicevutaTelematica.setDataOraMessaggioRicevuta(XmlUtil.toXMLGregoirianCalendar(now));
            ctRicevutaTelematica.setRiferimentoDataRichiesta(rpt.getRpt().getMessageRequestDatetime());
            ctRicevutaTelematica.setIstitutoAttestante(ctIstitutoAttestante);

            ctRicevutaTelematica.setEnteBeneficiario(ctEnteBeneficiario);
            ctRicevutaTelematica.setSoggettoPagatore(ctSoggettoPagatore);

            CtDatiVersamentoRT ctDatiVersamentoRT = objectFactory.createCtDatiVersamentoRT();
            ctDatiVersamentoRT.setCodiceEsitoPagamento("1");
            ctDatiVersamentoRT.setImportoTotalePagato(BigDecimal.ZERO);
            ctDatiVersamentoRT.setIdentificativoUnivocoVersamento(rpt.getRpt().getTransferData().getIuv());
            ctDatiVersamentoRT.setCodiceContestoPagamento(rpt.getRpt().getTransferData().getCcp());

            rpt.getRpt().getTransferData().getTransfer().stream().forEach(transfer -> {
                CtDatiSingoloPagamentoRT ctDatiSingoloPagamentoRT = objectFactory.createCtDatiSingoloPagamentoRT();
                ctDatiSingoloPagamentoRT.setSingoloImportoPagato(BigDecimal.ZERO);
                ctDatiSingoloPagamentoRT.setEsitoSingoloPagamento("Annullato da WISP");//TODO cosa mettere?
                ctDatiSingoloPagamentoRT.setDataEsitoSingoloPagamento(XmlUtil.toXMLGregoirianCalendar(now));
                ctDatiSingoloPagamentoRT.setIdentificativoUnivocoRiscossione("0");
                ctDatiSingoloPagamentoRT.setCausaleVersamento(transfer.getRemittanceInformation());
                ctDatiSingoloPagamentoRT.setDatiSpecificiRiscossione(transfer.getCategory());
                ctDatiVersamentoRT.getDatiSingoloPagamento().add(ctDatiSingoloPagamentoRT);
            });
        });

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
