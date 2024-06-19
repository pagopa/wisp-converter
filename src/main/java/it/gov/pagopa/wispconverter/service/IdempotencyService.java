package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.repository.IdempotencyKeyRepository;
import it.gov.pagopa.wispconverter.repository.model.IdempotencyKeyEntity;
import it.gov.pagopa.wispconverter.repository.model.enumz.IdempotencyStatusEnum;
import it.gov.pagopa.wispconverter.repository.model.enumz.ReceiptTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public boolean isIdempotencyKeyProcessable(String idempotencyKey, ReceiptTypeEnum receiptType) {
        boolean isProcessable = true;
        Optional<IdempotencyKeyEntity> optIdempotencyKeyEntity = idempotencyKeyRepository.findById(idempotencyKey);
        if(optIdempotencyKeyEntity.isPresent()) {
            IdempotencyKeyEntity idempotencyKeyEntity = optIdempotencyKeyEntity.get();
            if(!receiptType.equals(idempotencyKeyEntity.getReceiptType())) {
                throw new RuntimeException();
            }
            isProcessable = !IdempotencyStatusEnum.FAILED.equals(idempotencyKeyEntity.getStatus());
        }
        return isProcessable;
    }

    public void lockIdempotencyKey(String idempotencyKey, ReceiptTypeEnum receiptTypeEnum) {
        Optional<IdempotencyKeyEntity> optIdempotencyKeyEntity = idempotencyKeyRepository.findById(idempotencyKey);
        IdempotencyKeyEntity idempotencyKeyEntity;
        if(optIdempotencyKeyEntity.isPresent()) {
            idempotencyKeyEntity = optIdempotencyKeyEntity.get();
            if(!IdempotencyStatusEnum.FAILED.equals(idempotencyKeyEntity.getStatus())) {
                throw new RuntimeException();
            }
        } else {
            idempotencyKeyEntity = IdempotencyKeyEntity.builder()
                        .id(idempotencyKey)
                        .receiptType(receiptTypeEnum)
                        .build();
        }
        idempotencyKeyEntity.setStatus(IdempotencyStatusEnum.LOCKED);
        idempotencyKeyRepository.save(idempotencyKeyEntity);
    }

    public void unlockIdempotencyKey(String idempotencyKey, ReceiptTypeEnum receiptTypeEnum, IdempotencyStatusEnum status) {
        Optional<IdempotencyKeyEntity> optIdempotencyKeyEntity = idempotencyKeyRepository.findById(idempotencyKey);
        IdempotencyKeyEntity idempotencyKeyEntity;
        if(optIdempotencyKeyEntity.isPresent()) {
            idempotencyKeyEntity = optIdempotencyKeyEntity.get();
            if(!IdempotencyStatusEnum.LOCKED.equals(idempotencyKeyEntity.getStatus())) {
                throw new RuntimeException();
            }
        } else {
            idempotencyKeyEntity = IdempotencyKeyEntity.builder()
                    .id(idempotencyKey)
                    .receiptType(receiptTypeEnum)
                    .build();
        }
        idempotencyKeyEntity.setStatus(status);
        idempotencyKeyRepository.save(idempotencyKeyEntity);
    }
}
