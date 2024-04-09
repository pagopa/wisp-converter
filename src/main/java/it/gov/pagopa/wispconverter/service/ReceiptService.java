package it.gov.pagopa.wispconverter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.telematici.pagamenti.ws.ObjectFactory;
import gov.telematici.pagamenti.ws.PaaInviaRT;
import gov.telematici.pagamenti.ws.ppthead.IntestazionePPT;
import it.gov.digitpa.schemas._2011.pagamenti.*;
import it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigurationKeyDto;
import it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto;
import it.gov.pagopa.pagopa_api.pa.pafornode.PaSendRTV2Request;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.repository.RTRequestRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.repository.model.RTRequestEntity;
import it.gov.pagopa.wispconverter.service.mapper.RTMapper;
import it.gov.pagopa.wispconverter.service.model.CommonRPTFieldsDTO;
import it.gov.pagopa.wispconverter.service.model.RPTContentDTO;
import it.gov.pagopa.wispconverter.service.model.ReceiptDto;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.util.JaxbElementUtil;
import it.gov.pagopa.wispconverter.util.ReUtil;
import jakarta.xml.bind.JAXBElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;
import org.xmlsoap.schemas.soap.envelope.Envelope;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private final RptCosmosService rptCosmosService;
    private final RPTExtractorService rptExtractorService;
    private final ReService reService;

    private final RPTRequestRepository rptRequestRepository;
    private final RTRequestRepository rtRequestRepository;

    @Transactional
    public void paaInviaRTKo(String payload) {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        try {
            List<ReceiptDto> receiptDtos = List.of(mapper.readValue(payload, ReceiptDto[].class));
            ObjectFactory objectFactory = new ObjectFactory();

            Map<String, ConfigurationKeyDto> configurations = configCacheService.getConfigData().getConfigurations();

            receiptDtos.forEach(receipt -> {
                RPTRequestEntity rptRequestEntity = rptCosmosService.getRPTRequestEntity("intPaLorenz_75fa058f-d1b5-4c7e-865e-a220091d3954");
                String brokerPa = rptRequestEntity.getId().split("_")[0];

                CommonRPTFieldsDTO commonRPTFieldsDTO = this.rptExtractorService.extractRPTContentDTOs(rptRequestEntity.getPrimitive(), rptRequestEntity.getPayload());

                IntestazionePPT intestazionePPT = generateIntestazionePPT(
                        receipt.getIdentificativoDominio(),
                        receipt.getIdentificativoUnivocoVersamento(),
                        receipt.getPaymentToken(),
                        commonRPTFieldsDTO.getCreditorInstitutionBrokerId(),
                        commonRPTFieldsDTO.getStationId());

                commonRPTFieldsDTO.getRpts().forEach(rpt ->  {
                    Map<String, StationDto> stations = configCacheService.getConfigData().getStations();
                    StationDto stationDto = stations.get(rpt.getRpt().getDomain().getStationId());

                    Instant now = Instant.now();
                    JAXBElement<CtRicevutaTelematica> rt = new it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory().createRT(generateCtRicevutaTelematica(rpt, configurations, now));
                    String xmlString = jaxbElementUtil.convertToString(rt,CtRicevutaTelematica.class);

                    PaaInviaRT paaInviaRT = objectFactory.createPaaInviaRT();
                    paaInviaRT.setRt(Base64.getEncoder().encode(xmlString.getBytes(StandardCharsets.UTF_8)));

                    ReEventDto reInternal = ReUtil.createBaseReInternal()
                            .status("RT_GENERATA")
                            .ccp(rpt.getRpt().getTransferData().getCcp())
                            .canale("")
                            .erogatore("")
                            .erogatoreDescr("")
                            .idDominio(rpt.getRpt().getDomain().getDomainId())
                            .iuv(rpt.getIuv())
                            .noticeNumber(receipt.getIdentificativoUnivocoVersamento())
                            .paymentToken(receipt.getPaymentToken())
                            .psp(rpt.getRpt().getPayerInstitution().getSubjectUniqueIdentifier().getCode())
                            .pspDescr("")
                            .stazione(stationDto.getStationCode())
                            .standIn(false);
                    reService.addRe(reInternal);

                    JAXBElement<Envelope> envelope = jaxbElementUtil.createEnvelope(paaInviaRT, intestazionePPT);

                    RTRequestEntity rtRequestEntity = generateRTEntity(brokerPa, now, envelope, "");//TODO: impostare url stazione
                    rtRequestRepository.save(rtRequestEntity);
                });

            });
        } catch (JsonProcessingException e) {
            throw new AppException(AppErrorCodeMessageEnum.PARSING_INVALID_BODY);
        }
    }

    public void paaInviaRTOk(String payload) {
        try {
            Element envelopeElement = jaxbElementUtil.convertToEnvelopeElement(payload.getBytes(StandardCharsets.UTF_8));
            Envelope requestEnvelope = jaxbElementUtil.convertToBean(envelopeElement, Envelope.class);

            PaSendRTV2Request paSendRTV2Request = jaxbElementUtil.getSoapBody(requestEnvelope, PaSendRTV2Request.class);

            RPTRequestEntity rptRequestEntity = rptCosmosService.getRPTRequestEntity("intPaLorenz_75fa058f-d1b5-4c7e-865e-a220091d3954");

            CommonRPTFieldsDTO commonRPTFieldsDTO = this.rptExtractorService.extractRPTContentDTOs(rptRequestEntity.getPrimitive(), rptRequestEntity.getPayload());

            String brokerPa = rptRequestEntity.getId().split("_")[0];

            Map<String, StationDto> stations = configCacheService.getConfigData().getStations();
            StationDto stationDto = stations.get(paSendRTV2Request.getIdStation());

            ObjectFactory objectFactory = new ObjectFactory();

            commonRPTFieldsDTO.getRpts().forEach(rpt ->  {
                Instant now = Instant.now();

                IntestazionePPT intestazionePPT = generateIntestazionePPT(
                        paSendRTV2Request.getReceipt().getFiscalCode(),
                        paSendRTV2Request.getReceipt().getCreditorReferenceId(),
                        rpt.getRpt().getTransferData().getCcp(),
                        commonRPTFieldsDTO.getCreditorInstitutionBrokerId(),
                        commonRPTFieldsDTO.getStationId());

                JAXBElement<CtRicevutaTelematica> rt = new it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory().createRT(generateCtRicevutaTelematica(rpt, paSendRTV2Request));
                String xmlString = jaxbElementUtil.convertToString(rt,CtRicevutaTelematica.class);

                PaaInviaRT paaInviaRT = objectFactory.createPaaInviaRT();
                paaInviaRT.setRt(Base64.getEncoder().encode(xmlString.getBytes(StandardCharsets.UTF_8)));

                ReEventDto reInternal = ReUtil.createBaseReInternal()
                        .status("RT_GENERATA");
                reService.addRe(reInternal);

                JAXBElement<Envelope> envelope = jaxbElementUtil.createEnvelope(paaInviaRT, intestazionePPT);

                RTRequestEntity rtRequestEntity = generateRTEntity(brokerPa, now, envelope, "");//TODO: impostare url stazione
                rtRequestRepository.save(rtRequestEntity);
            });

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(AppErrorCodeMessageEnum.GENERIC_ERROR);
        }
    }

    private CtRicevutaTelematica generateCtRicevutaTelematica(RPTContentDTO rpt, Map<String, ConfigurationKeyDto> configurations, Instant now) {
        it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory objectFactory = new it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory();
        CtRicevutaTelematica ctRicevutaTelematica = objectFactory.createCtRicevutaTelematica();

        CtIstitutoAttestante ctIstitutoAttestante = objectFactory.createCtIstitutoAttestante();
        CtIdentificativoUnivoco ctIdentificativoUnivoco = objectFactory.createCtIdentificativoUnivoco();
        rtMapper.toCtIstitutoAttestante(ctIstitutoAttestante, ctIdentificativoUnivoco, configurations);

        CtDominio ctDominio = objectFactory.createCtDominio();
        rtMapper.toCtDominio(ctDominio, rpt.getRpt().getDomain());

        CtEnteBeneficiario ctEnteBeneficiario = objectFactory.createCtEnteBeneficiario();
        rtMapper.toCtEnteBeneficiario(ctEnteBeneficiario, rpt.getRpt().getPayerInstitution());

        CtSoggettoPagatore ctSoggettoPagatore = objectFactory.createCtSoggettoPagatore();
        rtMapper.toCtSoggettoPagatore(ctSoggettoPagatore, rpt.getRpt().getPayer());

        CtDatiVersamentoRT ctDatiVersamentoRT = objectFactory.createCtDatiVersamentoRT();
        rtMapper.toCtDatiVersamentoRT(ctDatiVersamentoRT, rpt.getRpt().getTransferData(), now);

        rtMapper.toCtRicevutaTelematicaNegativa(ctRicevutaTelematica, rpt.getRpt(), now);

        ctRicevutaTelematica.setDominio(ctDominio);
        ctRicevutaTelematica.setIstitutoAttestante(ctIstitutoAttestante);
        ctRicevutaTelematica.setEnteBeneficiario(ctEnteBeneficiario);
        ctRicevutaTelematica.setSoggettoPagatore(ctSoggettoPagatore);
        ctRicevutaTelematica.setDatiPagamento(ctDatiVersamentoRT);

        return ctRicevutaTelematica;
    }

    private CtRicevutaTelematica generateCtRicevutaTelematica(RPTContentDTO rpt, PaSendRTV2Request paSendRTV2Request) {
        it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory objectFactory = new it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory();
        CtRicevutaTelematica ctRicevutaTelematica = objectFactory.createCtRicevutaTelematica();

        CtIstitutoAttestante ctIstitutoAttestante = objectFactory.createCtIstitutoAttestante();
        rtMapper.toCtIstitutoAttestante(ctIstitutoAttestante, paSendRTV2Request);

        CtDominio ctDominio = objectFactory.createCtDominio();
        rtMapper.toCtDominio(ctDominio, rpt.getRpt().getDomain());

        CtEnteBeneficiario ctEnteBeneficiario = objectFactory.createCtEnteBeneficiario();
        rtMapper.toCtEnteBeneficiario(ctEnteBeneficiario, rpt.getRpt().getPayerInstitution());

        CtSoggettoPagatore ctSoggettoPagatore = objectFactory.createCtSoggettoPagatore();
        rtMapper.toCtSoggettoPagatore(ctSoggettoPagatore, paSendRTV2Request.getReceipt().getDebtor());

        rtMapper.toCtRicevutaTelematicaPositiva(ctRicevutaTelematica, rpt.getRpt(), paSendRTV2Request);

        CtDatiVersamentoRT ctDatiVersamentoRT = objectFactory.createCtDatiVersamentoRT();
        rtMapper.toCtDatiVersamentoRT(ctDatiVersamentoRT, rpt.getRpt().getTransferData(), paSendRTV2Request.getReceipt());

        ctRicevutaTelematica.setDominio(ctDominio);
        ctRicevutaTelematica.setIstitutoAttestante(ctIstitutoAttestante);
        ctRicevutaTelematica.setEnteBeneficiario(ctEnteBeneficiario);
        ctRicevutaTelematica.setSoggettoPagatore(ctSoggettoPagatore);
        ctRicevutaTelematica.setDatiPagamento(ctDatiVersamentoRT);

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

    private RTRequestEntity generateRTEntity(String brokerPa, Instant now, JAXBElement<Envelope> envelope, String url) {
        return RTRequestEntity
                .builder()
                .id(brokerPa+"_"+UUID.randomUUID())
                .primitive("paaInviaRT")
                .partitionKey(LocalDate.ofInstant(now, ZoneId.systemDefault()).toString())
                .payload(jaxbElementUtil.convertToString(envelope, Envelope.class))
                .url(url)
                .retry(0)
                .build();
    }

//    private String getStationUrl(StationDto stationDto) {
//        String connection = stationDto.getConnection().getProtocol();
//    }

}
