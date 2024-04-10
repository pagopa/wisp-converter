package it.gov.pagopa.wispconverter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
//    private final ObjectMapper objectMapper;

    public void addRe(ReEventDto reEventDto) {
//        try {
//            log.info("\n" +
//                    "#################\n" +
//                    "# RE "+reEventDto.getCategoriaEvento()+"/"+reEventDto.getCallType()+"/"+reEventDto.getSottoTipoEvento()+" \n" +
//                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(reEventDto) + "\n" +
//                    "#################");
            ReEventEntity reEventEntity = reEventMapper.toReEventEntity(reEventDto);
            reEventRepository.save(reEventEntity);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
    }


}
