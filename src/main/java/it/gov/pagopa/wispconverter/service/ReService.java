package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.ReEventRepository;
import it.gov.pagopa.wispconverter.repository.model.ReEventEntity;
import it.gov.pagopa.wispconverter.service.mapper.ReEventMapper;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReService {

    private final ReEventRepository reEventRepository;
    private final ReEventMapper reEventMapper;

    public void addRe(ReEventDto reEventDto) {
        try {
            ReEventEntity reEventEntity = reEventMapper.toReEventEntity(reEventDto);
            reEventRepository.save(reEventEntity);
        } catch (Exception e) {
            throw new AppException(AppErrorCodeMessageEnum.PERSISTENCE_SAVING_RE_ERROR, e);
        }
    }
}
