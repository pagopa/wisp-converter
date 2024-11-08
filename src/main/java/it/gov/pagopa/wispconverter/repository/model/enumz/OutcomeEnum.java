package it.gov.pagopa.wispconverter.repository.model.enumz;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum OutcomeEnum {
    COMMUNICATION_RECEIVED_FAILURE("failure during external communication"),
    OK("Operation completed successfully"),
    RPT_TIMER_CREATION_FAILED("Failed to create the report timer"),
    CONVERSION_ERROR_SENDING_RT("Error occurred while converting or sending the report"),
    SENDING_RT_FAILED_NOT_STORED_IN_DEADLETTER(
            "Sending the report failed and it was not stored in the dead-letter queue"),
    SENDING_RT_FAILED_REJECTED_BY_CI("Sending the report failed due to rejection by the CI system"),
    SENDING_RT_FAILED_RESCHEDULING_SUCCESSFUL(
            "Report sending failed, but rescheduling was successful"),
    SENDING_RT_FAILED_RESCHEDULING_FAILED("Report sending failed and rescheduling also failed"),
    SENDING_RT_FAILED_MAX_RETRIES("Report sending failed after maximum retries were reached"),
    KO("Error during endpoint call");

    private final String description;
}
