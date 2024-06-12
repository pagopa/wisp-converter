package it.gov.pagopa.wispconverter.repository.model.enumz;

import io.swagger.v3.oas.annotations.media.Schema;

public enum OutcomeEnum {

    @Schema(description = "Correctly sent request to HTTP endpoint. In NDP it is mapped with value 'INVIATA'.")
    SEND,

    @Schema(description = "Failed to send request to HTTP endpoint. In NDP it is mapped with value 'INVIATA_KO'")
    SEND_FAILURE,

    @Schema(description = "Received an OK response from HTTP endpoint. In NDP it is mapped with value 'RICEVUTA_KO'")
    RECEIVED,

    @Schema(description = "Received a failure response from endpoint. In NDP it is mapped with value 'RICEVUTA_KO'")
    RECEIVED_FAILURE,

    @Schema(description = "Failed to receive response at all from endpoint. In NDP it is mapped with value 'NO_RICEVUTA'")
    NEVER_RECEIVED,

    @Schema(description = "Executed internal step on execution. In NDP it is mapped with value 'CAMBIO_STATO'")
    EXECUTED_INTERNAL_STEP,
}
