package it.gov.pagopa.wispconverter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.wispconverter.controller.model.RPTTimerRequest;
import it.gov.pagopa.wispconverter.repository.model.enumz.OutcomeEnum;
import it.gov.pagopa.wispconverter.repository.model.enumz.WorkflowStatus;
import it.gov.pagopa.wispconverter.service.RPTTimerService;
import it.gov.pagopa.wispconverter.util.EndpointRETrace;
import it.gov.pagopa.wispconverter.util.ErrorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rpt")
@Validated
@RequiredArgsConstructor
@Tag(name = "RPTTimer", description = "Create and Delete rpt timer")
@Slf4j
public class RPTTimerController {
    private static final String RPT_BP_TIMER_SET = "rpt-timer-set";
    private static final String RPT_BP_TIMER_DELETE = "rpt-timer-delete";

    private final RPTTimerService rptTimerService;

    private final ErrorUtil errorUtil;


    @Operation(summary = "createRPTTimer", description = "Create a timer from sessionId data", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"RPTTimer"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully rpt timer created", content = @Content(schema = @Schema()))
    })
    @PostMapping(
            value = "/timer",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @EndpointRETrace(status = WorkflowStatus.RPT_TIMER_CREATION_PROCESSED, outcomeError = OutcomeEnum.RPT_TIMER_CREATION_FAILED, businessProcess = RPT_BP_TIMER_SET, reEnabled = true)
    public void createTimer(@RequestBody RPTTimerRequest request) {
            rptTimerService.sendMessage(request);
    }

    @Operation(summary = "deleteRPTTimer", description = "Delete a timer by sessionId", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"RPTTimer"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully rpt timer deleted", content = @Content(schema = @Schema()))
    })
    @DeleteMapping(
            value = "/timer",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @EndpointRETrace(status = WorkflowStatus.RPT_TIMER_DELETION_PROCESSED, outcomeError = OutcomeEnum.RPT_TIMER_DELETION_FAILED, businessProcess = RPT_BP_TIMER_DELETE, reEnabled = true)
    public void deleteTimer(@RequestParam() String sessionId) {
            rptTimerService.cancelScheduledMessage(sessionId);
    }
}
