package it.gov.pagopa.wispconverter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.wispconverter.controller.model.ReceiptTimerRequest;
import it.gov.pagopa.wispconverter.service.ReceiptTimerService;
import it.gov.pagopa.wispconverter.util.Trace;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/receipt")
@Validated
@RequiredArgsConstructor
@Tag(name = "ReceiptTimer", description = "Create and Delete payment token timer")
@Slf4j
public class ReceiptTimerController {
    private static final String BP_TIMER_SET = "timer-set";
    private static final String BP_TIMER_DELETE = "timer-delete";

    private final ReceiptTimerService receiptTimerService;

    @Operation(summary = "createTimer", description = "Create a timer linked with paymentToken and receipt data", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"ReceiptTimer"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully paymentToken expiration timer created", content = @Content(schema = @Schema()))
    })
    @PostMapping(
            value = "/timer",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @Trace(businessProcess = BP_TIMER_SET, reEnabled = true)
    public void createTimer(@RequestBody ReceiptTimerRequest request) {
        log.info("Set Timer arrived: " + request.toString());
        receiptTimerService.sendMessage(request);
    }

    @Operation(summary = "deleteTimer", description = "Delete a timer by paymentToken", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"ReceiptTimer"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully paymentToken expiration timer deleted", content = @Content(schema = @Schema()))
    })
    @DeleteMapping(
            value = "/timer",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Trace(businessProcess = BP_TIMER_DELETE, reEnabled = true)
  public void deleteTimer(@RequestParam() String tokens) {
        List<String> paymentTokens = Arrays.asList(tokens.split(","));
        log.info("Delete Timer arrived: " + paymentTokens);
        receiptTimerService.cancelScheduledMessage(paymentTokens);
    }
}
