package it.gov.pagopa.wispconverter.repository.model.enumz;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum OutcomeEnum {
    OK("Operation completed successfully"),
    ERROR("Generic error occurred during execution"),
    SENDING_RT_FAILED_STORED_IN_DEADLETTER(""),
    SENDING_RT_FAILED_REJECTED_BY_CI(""),
    SENDING_RT_FAILED_RESCHEDULED_SUCCESSFULLY(""),
    SENDING_RT_FAILED_NOT_RESCHEDULED_DUE_ERROR(""),
    SENDING_RT_FAILED_NOT_RESCHEDULED_DUE_MAX_RETRIES(""),
    COMMUNICATION_FAILURE(""),
    SENDING_RT_FAILED("");

    private final String description;


}
