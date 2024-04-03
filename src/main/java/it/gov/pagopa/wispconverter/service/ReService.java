package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.service.model.ConversionResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReService {
    public void addRe(String dir) {
        log.info("\n#################\n# RE INTERFACE "+dir+"\n#################");
    }
}
