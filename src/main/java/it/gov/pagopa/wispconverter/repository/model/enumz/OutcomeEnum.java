package it.gov.pagopa.wispconverter.repository.model.enumz;

import io.swagger.v3.oas.annotations.media.Schema;

public enum OutcomeEnum {

    OK,

    @Schema(description = "Received a failure response from endpoint. In NDP it is mapped with value 'RICEVUTA_KO'")
    COMMUNICATION_RECEIVED_FAILURE,

    @Schema(description = "Failed to receive response at all from endpoint. In NDP it is mapped with value 'NO_RICEVUTA'")
    COMMUNICATION_NEVER_RECEIVED,

    SENDING_RT_FAILED_NOT_STORED_IN_DEADLETTER
}
