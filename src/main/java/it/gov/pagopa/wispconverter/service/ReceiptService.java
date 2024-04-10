package it.gov.pagopa.wispconverter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.telematici.pagamenti.ws.nodoperpa.ppthead.IntestazionePPT;
import gov.telematici.pagamenti.ws.pafornode.PaSendRTV2Request;
import gov.telematici.pagamenti.ws.papernodo.PaaInviaRT;
import it.gov.digitpa.schemas._2011.pagamenti.*;
import it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigurationKeyDto;
import it.gov.pagopa.gen.wispconverter.client.cache.model.ConnectionDto;
import it.gov.pagopa.gen.wispconverter.client.cache.model.PaymentServiceProviderDto;
import it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto;
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
import it.gov.pagopa.wispconverter.service.model.re.EntityStatusEnum;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.util.AppBase64Util;
import it.gov.pagopa.wispconverter.util.JaxbElementUtil;
import it.gov.pagopa.wispconverter.util.ReUtil;
import it.gov.pagopa.wispconverter.util.ZipUtil;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.soap.SOAPMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static it.gov.pagopa.wispconverter.util.Constants.NODO_DEI_PAGAMENTI_SPC;
import static it.gov.pagopa.wispconverter.util.Constants.PA_INVIA_RT;

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

    private final ObjectMapper mapper;

    @Transactional
    public void paaInviaRTKo(String payload) {
        try {
            List<ReceiptDto> receiptDtos = List.of(mapper.readValue(payload, ReceiptDto[].class));
            gov.telematici.pagamenti.ws.papernodo.ObjectFactory objectFactory = new gov.telematici.pagamenti.ws.papernodo.ObjectFactory();

            Map<String, ConfigurationKeyDto> configurations = configCacheService.getConfigData().getConfigurations();
            Map<String, PaymentServiceProviderDto> psps = configCacheService.getConfigData().getPsps();
            Map<String, StationDto> stations = configCacheService.getConfigData().getStations();

            receiptDtos.forEach(receipt -> {
                //TODO: interrogare Redis per recuperare sessionId da usare su Cosmos
                String sessionIdEntity = "intPaLorenz_75fa058f-d1b5-4c7e-865e-a220091d3954";
                String brokerPa = sessionIdEntity.split("_")[0];

                RPTRequestEntity rptRequestEntity = rptCosmosService.getRPTRequestEntity(sessionIdEntity);

                CommonRPTFieldsDTO commonRPTFieldsDTO = this.rptExtractorService.extractRPTContentDTOs(rptRequestEntity.getPrimitive(), rptRequestEntity.getPayload());

                IntestazionePPT intestazionePPT = generateIntestazionePPT(
                        receipt.getIdentificativoDominio(),
                        receipt.getIdentificativoUnivocoVersamento(),
                        receipt.getPaymentToken(),
                        commonRPTFieldsDTO.getCreditorInstitutionBrokerId(),
                        commonRPTFieldsDTO.getStationId());

                commonRPTFieldsDTO.getRpts().forEach(rpt ->  {
                    StationDto stationDto = stations.get(commonRPTFieldsDTO.getStationId());

                    Instant now = Instant.now();
                    JAXBElement<CtRicevutaTelematica> rt = new it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory().createRT(generateCtRicevutaTelematica(rpt, configurations, now));
                    String xmlString = jaxbElementUtil.objectToString(rt);

                    PaaInviaRT paaInviaRT = objectFactory.createPaaInviaRT();
                    paaInviaRT.setRt(AppBase64Util.base64Encode(xmlString.getBytes(StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8));
                    JAXBElement<PaaInviaRT> paaInviaRTJaxb = objectFactory.createPaaInviaRT(paaInviaRT);

                    SOAPMessage message = jaxbElementUtil.newMessage();
                    jaxbElementUtil.addBody(message, paaInviaRTJaxb, PaaInviaRT.class);
                    jaxbElementUtil.addHeader(message, intestazionePPT, IntestazionePPT.class);

                    RTRequestEntity rtRequestEntity = null;
                    try {
                        rtRequestEntity = generateRTEntity(brokerPa, now, jaxbElementUtil.toString(message), getStationUrl(stationDto));
                    } catch (IOException e) {
                        throw new AppException(AppErrorCodeMessageEnum.PARSING_GENERIC_ERROR);
                    }
                    rtRequestRepository.save(rtRequestEntity);

                    PaymentServiceProviderDto psp = psps.get(rpt.getRpt().getPayerInstitution().getSubjectUniqueIdentifier().getCode());

                    //generate and save re event internal for change status
                    ReEventDto reEventDto = generateReInternal(rptRequestEntity, rpt, receipt.getIdentificativoUnivocoVersamento(), receipt.getPaymentToken(), stationDto, psp);
                    reService.addRe(reEventDto);
                });
            });
        } catch (JsonProcessingException e) {
            throw new AppException(AppErrorCodeMessageEnum.PARSING_INVALID_BODY);
        } catch (Exception e) {
            throw new AppException(AppErrorCodeMessageEnum.GENERIC_ERROR);
        }
    }

    @Transactional
    public void paaInviaRTOk(String payload) {
        try {
            Map<String, StationDto> stations = configCacheService.getConfigData().getStations();
            Map<String, PaymentServiceProviderDto> psps = configCacheService.getConfigData().getPsps();

            SOAPMessage envelopeElement = jaxbElementUtil.getMessage(payload);
            PaSendRTV2Request paSendRTV2Request = jaxbElementUtil.getBody(envelopeElement, PaSendRTV2Request.class);

            //TODO: interrogare Redis per recuperare sessionId da usare su Cosmos
            String sessionIdEntity = "intPaLorenz_75fa058f-d1b5-4c7e-865e-a220091d3954";
            String brokerPa = sessionIdEntity.split("_")[0];

            RPTRequestEntity rptRequestEntity = rptCosmosService.getRPTRequestEntity("intPaLorenz_75fa058f-d1b5-4c7e-865e-a220091d3954");

            CommonRPTFieldsDTO commonRPTFieldsDTO = this.rptExtractorService.extractRPTContentDTOs(rptRequestEntity.getPrimitive(), rptRequestEntity.getPayload());

            StationDto stationDto = stations.get(paSendRTV2Request.getIdStation());

            gov.telematici.pagamenti.ws.papernodo.ObjectFactory objectFactory = new gov.telematici.pagamenti.ws.papernodo.ObjectFactory();

            commonRPTFieldsDTO.getRpts().forEach(rpt ->  {
                Instant now = Instant.now();

                IntestazionePPT intestazionePPT = generateIntestazionePPT(
                        paSendRTV2Request.getReceipt().getFiscalCode(),
                        paSendRTV2Request.getReceipt().getCreditorReferenceId(),
                        rpt.getRpt().getTransferData().getCcp(),
                        commonRPTFieldsDTO.getCreditorInstitutionBrokerId(),
                        commonRPTFieldsDTO.getStationId());

                JAXBElement<CtRicevutaTelematica> rt = new it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory().createRT(generateCtRicevutaTelematica(rpt, paSendRTV2Request));
                String xmlString = jaxbElementUtil.objectToString(rt);

                PaaInviaRT paaInviaRT = objectFactory.createPaaInviaRT();
                paaInviaRT.setRt(AppBase64Util.base64Encode(xmlString.getBytes(StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8));
                JAXBElement<PaaInviaRT> paaInviaRTJaxb = objectFactory.createPaaInviaRT(paaInviaRT);

                SOAPMessage message = jaxbElementUtil.newMessage();
                jaxbElementUtil.addBody(message, paaInviaRTJaxb, PaaInviaRT.class);
                jaxbElementUtil.addHeader(message, intestazionePPT, IntestazionePPT.class);

                RTRequestEntity rtRequestEntity = null;
                try {
                    rtRequestEntity = generateRTEntity(brokerPa, now, jaxbElementUtil.toString(message), getStationUrl(stationDto));
                } catch (IOException e) {
                    throw new AppException(AppErrorCodeMessageEnum.PARSING_GENERIC_ERROR);
                }
                rtRequestRepository.save(rtRequestEntity);

                PaymentServiceProviderDto psp = psps.get(rpt.getRpt().getPayerInstitution().getSubjectUniqueIdentifier().getCode());

                //generate and save re event internal for change status
                ReEventDto reEventDto = generateReInternal(rptRequestEntity,
                        rpt,
                        paSendRTV2Request.getReceipt().getNoticeNumber(),
                        paSendRTV2Request.getReceipt().getCreditorReferenceId(),
                        stationDto,
                        psp);
                reService.addRe(reEventDto);
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
        gov.telematici.pagamenti.ws.nodoperpa.ppthead.ObjectFactory objectFactoryHead =
                new gov.telematici.pagamenti.ws.nodoperpa.ppthead.ObjectFactory();

        IntestazionePPT header = objectFactoryHead.createIntestazionePPT();
        header.setIdentificativoDominio(idDominio);
        header.setIdentificativoUnivocoVersamento(iuv);
        header.setCodiceContestoPagamento(ccp);
        header.setIdentificativoIntermediarioPA(idIntermediarioPa);
        header.setIdentificativoStazioneIntermediarioPA(idStazione);
        return header;
    }

    private RTRequestEntity generateRTEntity(String brokerPa, Instant now, String payload, String url) throws IOException {
        return RTRequestEntity
                .builder()
                .id(brokerPa+"_"+UUID.randomUUID())
                .primitive(PA_INVIA_RT)
                .partitionKey(LocalDate.ofInstant(now, ZoneId.systemDefault()).toString())
                .payload(AppBase64Util.base64Encode(ZipUtil.zip(payload)))
                .url(url)
                .retry(0)
                .build();
    }

    private String getStationUrl(StationDto stationDto) {
        ConnectionDto.ProtocolEnum connection = stationDto.getConnection().getProtocol();
        String ip = stationDto.getConnection().getIp();
        Long port = stationDto.getConnection().getPort();
        String path = stationDto.getService().getPath();
        return "";
    }

    private ReEventDto generateReInternal(RPTRequestEntity rptRequestEntity,
                                          RPTContentDTO rptContentDTO,
                                          String noticeNumber,
                                          String paymentToken,
                                          StationDto station,
                                          PaymentServiceProviderDto psp) {
        ReEventDto reEventDto = ReUtil.createBaseReInternal()
                .status(EntityStatusEnum.RT_GENERATA.name())
                .erogatore(NODO_DEI_PAGAMENTI_SPC)
                .erogatoreDescr(NODO_DEI_PAGAMENTI_SPC)
                .sessionIdOriginal(rptRequestEntity.getId())
                .ccp(rptContentDTO.getRpt().getTransferData().getCcp())
                .idDominio(rptContentDTO.getRpt().getDomain().getDomainId())
                .iuv(rptContentDTO.getIuv())
                .noticeNumber(noticeNumber)
                .paymentToken(paymentToken);

        if( psp != null ) {
            reEventDto.psp(psp.getPspCode());
            reEventDto.pspDescr(psp.getDescription());
        }
        if( station != null ) {
            reEventDto.stazione(station.getStationCode());
        }
//        ReUtil.createBaseReInternal()
//                .canale("")//TODO
//                .standIn(false);//TODO vale la pena aggiungere anche parte standin da configurazione?
        return reEventDto;
    }

}
