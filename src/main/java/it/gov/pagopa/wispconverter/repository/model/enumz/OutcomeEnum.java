package it.gov.pagopa.wispconverter.repository.model.enumz;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum OutcomeEnum {
    OK("Operation completed successfully"),
    COMMUNICATION_RECEIVED_FAILURE("failure during external communication"),
    RPT_TIMER_CREATION_FAILED("Failed to create the report timer"),
    PAYMENT_TOKEN_TIMER_CREATION_FAILED("Failed to create the payment token timer"),
    PAYMENT_TOKEN_TIMER_DELETION_FAILED("Failed to delete the payment token timer"),
    RPT_TIMER_DELETION_FAILED("Failed to delete the report timer"),
    CONVERSION_ERROR_SENDING_RT("rror occurred while converting or sending the report"),
    SENDING_RT_FAILED_NOT_STORED_IN_DEADLETTER(
            "ending the report failed and it was not stored in the dead-letter queue"),
    SENDING_RT_FAILED_REJECTED_BY_CI("ending the report failed due to rejection by the CI system"),
    SENDING_RT_FAILED_RESCHEDULING_SUCCESSFUL(
            "eport sending failed, but rescheduling was successful"),
    SENDING_RT_FAILED_RESCHEDULING_FAILED("eport sending failed and rescheduling also failed"),
    SENDING_RT_FAILED_MAX_RETRIES("eport sending failed after maximum retries were reached"),
    KO("rror during endpoint call");

    private final String description;


}
