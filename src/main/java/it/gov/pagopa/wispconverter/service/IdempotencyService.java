package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.IdempotencyKeyRepository;
import it.gov.pagopa.wispconverter.repository.model.IdempotencyKeyEntity;
import it.gov.pagopa.wispconverter.repository.model.enumz.IdempotencyStatusEnum;
import it.gov.pagopa.wispconverter.repository.model.enumz.ReceiptTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Value("${wisp-converter.idempotency.lock-validity-in-minutes}")
    private Integer lockValidityInMinutes;

    public static String generateIdempotencyKeyId(String sessionId, String iuv, String domainId) {
        return String.format("%s_%s_%s", sessionId, iuv, domainId);
    }

    public boolean isIdempotencyKeyProcessable(String idempotencyKey, ReceiptTypeEnum receiptType) {

        boolean isProcessable = true;

        // try to retrieve idempotency key entity from the storage and check if exists
        Optional<IdempotencyKeyEntity> optIdempotencyKeyEntity = idempotencyKeyRepository.findById(idempotencyKey);
        if (optIdempotencyKeyEntity.isPresent()) {

            /*
              Check if receipt type (set in idempotency key) is equals to the one defined in the receipt entity.
              If they are not equals it is an anomaly, so throw a dedicated exception
             */
            IdempotencyKeyEntity idempotencyKeyEntity = optIdempotencyKeyEntity.get();
            if (!receiptType.equals(idempotencyKeyEntity.getReceiptType())) {
                throw new AppException(AppErrorCodeMessageEnum.RECEIPT_GENERATION_ANOMALY_ON_PROCESSING, idempotencyKey);
            }

            // check the processability of the idempotency key
            isProcessable = !IdempotencyStatusEnum.SUCCESS.equals(idempotencyKeyEntity.getStatus()) && isActiveLockExpired(idempotencyKeyEntity) || IdempotencyStatusEnum.FAILED.equals(idempotencyKeyEntity.getStatus());
        }
        return isProcessable;
    }

    public boolean isCompleted(String idempotencyKey) {

        boolean isSucceeded = false;

        // try to retrieve idempotency key entity from the storage and check if exists
        Optional<IdempotencyKeyEntity> optIdempotencyKeyEntity = idempotencyKeyRepository.findById(idempotencyKey);
        if (optIdempotencyKeyEntity.isPresent()) {

            // check if the idempotency key is in a success status
            isSucceeded = IdempotencyStatusEnum.SUCCESS.equals(optIdempotencyKeyEntity.get().getStatus());
        }

        return isSucceeded;
    }

    public void lockIdempotencyKey(String idempotencyKey, ReceiptTypeEnum receiptTypeEnum) {

        IdempotencyKeyEntity idempotencyKeyEntity;

        // try to retrieve idempotency key entity from the storage and check if exists
        // In this case, no findBy with partition key is set because the search could refers to different days
        Optional<IdempotencyKeyEntity> optIdempotencyKeyEntity = idempotencyKeyRepository.findById(idempotencyKey);
        if (optIdempotencyKeyEntity.isPresent()) {

            // check either if a lock exists and is active and if the status is not failed
            idempotencyKeyEntity = optIdempotencyKeyEntity.get();
            if (!isActiveLockExpired(idempotencyKeyEntity)) {

                throw new AppException(AppErrorCodeMessageEnum.RECEIPT_GENERATION_IDEMPOTENCY_LOCKED_BY_ANOTHER_PROCESS, idempotencyKey);

            } else if (!IdempotencyStatusEnum.FAILED.equals(idempotencyKeyEntity.getStatus())) {

                throw new AppException(AppErrorCodeMessageEnum.RECEIPT_GENERATION_NOT_PROCESSABLE, idempotencyKey, idempotencyKeyEntity.getStatus());
            }

        } else {

            // extracting sessionId for entity
            String sessionId = null;
            String[] idempotencyKeyComponents = idempotencyKey.split("_");
            if (idempotencyKeyComponents.length > 0) {
                sessionId = idempotencyKeyComponents[0];
            }

            // it not exists, so it can be created a new idempotency key entity ex novo
            idempotencyKeyEntity = IdempotencyKeyEntity.builder()
                    .id(idempotencyKey)
                    .receiptType(receiptTypeEnum)
                    .sessionId(sessionId)
                    .build();
        }

        // persist the changes made
        idempotencyKeyEntity.setStatus(IdempotencyStatusEnum.LOCKED);
        idempotencyKeyEntity.setLockedAt(Instant.now());
        idempotencyKeyRepository.save(idempotencyKeyEntity);
    }

    public void unlockIdempotencyKey(String idempotencyKey, ReceiptTypeEnum receiptTypeEnum, IdempotencyStatusEnum status) {

        IdempotencyKeyEntity idempotencyKeyEntity;

        // try to retrieve idempotency key entity from the storage and check if exists
        Optional<IdempotencyKeyEntity> optIdempotencyKeyEntity = idempotencyKeyRepository.findById(idempotencyKey);
        if (optIdempotencyKeyEntity.isPresent()) {

            // check if it is not in a locked state
            idempotencyKeyEntity = optIdempotencyKeyEntity.get();
            if (!IdempotencyStatusEnum.LOCKED.equals(idempotencyKeyEntity.getStatus())) {

                throw new AppException(AppErrorCodeMessageEnum.RECEIPT_GENERATION_ALREADY_PROCESSED, idempotencyKey);
            }

        } else {

            // it not exists, so it can be created a new idempotency key entity ex novo with the required state
            idempotencyKeyEntity = IdempotencyKeyEntity.builder()
                    .id(idempotencyKey)
                    .receiptType(receiptTypeEnum)
                    .build();
        }

        // persist the changes made
        idempotencyKeyEntity.setStatus(status);
        idempotencyKeyEntity.setLockedAt(null);
        idempotencyKeyRepository.save(idempotencyKeyEntity);
    }

    private boolean isActiveLockExpired(IdempotencyKeyEntity idempotencyKeyEntity) {

        boolean isExpired = true;
        Instant lockedAt = idempotencyKeyEntity.getLockedAt();
        if (lockedAt != null) {
            Instant lockExpirationTime = lockedAt.plus(lockValidityInMinutes, ChronoUnit.MINUTES);
            isExpired = IdempotencyStatusEnum.LOCKED.equals(idempotencyKeyEntity.getStatus()) && lockExpirationTime.isBefore(Instant.now());
        }
        return isExpired;
    }
}
