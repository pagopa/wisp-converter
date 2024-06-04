package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.repository.RTRequestRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.repository.model.RTRequestEntity;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.service.model.session.RPTContentDTO;
import it.gov.pagopa.wispconverter.util.ReUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static it.gov.pagopa.wispconverter.util.Constants.NODO_DEI_PAGAMENTI_SPC;

@Service
@Slf4j
@RequiredArgsConstructor
public class RTCosmosService {

    private final RTRequestRepository rtRequestRepository;

    @Transactional
    public void saveRTRequestEntity(RTRequestEntity rtRequestEntity) {
        rtRequestRepository.save(rtRequestEntity);
    }

    public ReEventDto generateRE(RPTRequestEntity rptRequestEntity,
                                 RPTContentDTO rptContentDTO,
                                 String noticeNumber,
                                 String paymentToken,
                                 String stationCode,
                                 it.gov.pagopa.gen.wispconverter.client.cache.model.PaymentServiceProviderDto psp,
                                 String entityStatusName
    ) {
        ReEventDto.ReEventDtoBuilder reEventDtoBuilder = ReUtil.createBaseReInternal()
                .status(entityStatusName)
                .erogatore(NODO_DEI_PAGAMENTI_SPC)
                .erogatoreDescr(NODO_DEI_PAGAMENTI_SPC)
                .sessionIdOriginal(rptRequestEntity.getId())
                .ccp(rptContentDTO.getRpt().getTransferData().getCcp())
                .idDominio(rptContentDTO.getRpt().getDomain().getDomainId())
                .iuv(rptContentDTO.getIuv())
                .noticeNumber(noticeNumber)
                .paymentToken(paymentToken);

        if (psp != null) {
            reEventDtoBuilder.psp(psp.getPspCode());
            reEventDtoBuilder.pspDescr(psp.getDescription());
        }
        if (stationCode != null) {
            reEventDtoBuilder.stazione(stationCode);
        }
        return reEventDtoBuilder.build();
    }
}
