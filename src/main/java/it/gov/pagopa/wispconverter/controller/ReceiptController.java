package it.gov.pagopa.wispconverter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.wispconverter.controller.model.ReceiptRequest;
import it.gov.pagopa.wispconverter.controller.model.ReceiptTimerRequest;
import it.gov.pagopa.wispconverter.service.ReceiptService;
import it.gov.pagopa.wispconverter.service.ReceiptTimerService;
import it.gov.pagopa.wispconverter.util.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/receipt")
@Validated
@RequiredArgsConstructor
@Tag(name = "Receipt", description = "Convert sendPaymentResultV2, closePaymentV2 or paSendRTV2 into paaInviaRT to EC")
@Slf4j
public class ReceiptController {

    private static final String BP_RECEIPT_OK = "receipt-ok";
    private static final String BP_RECEIPT_KO = "receipt-ko";
    private static final String BP_TIMER_SET = "timer-set";
    private static final String BP_TIMER_DELETE = "timer-delete";
    private final ReceiptService receiptService;

    private final ReceiptTimerService receiptTimerService;

    @Operation(summary = "", description = "", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Receipt"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully forwarded paaInviaRT- to EC", content = @Content(schema = @Schema()))
    })
    @PostMapping(
            value = "/ko",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @Trace(businessProcess = BP_RECEIPT_KO, reEnabled = true)
    public void receiptKo(@RequestBody ReceiptRequest request) throws IOException {

        receiptService.paaInviaRTKo(request.getContent());
    }

    @Operation(summary = "", description = "", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Receipt"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully forwarded paaInviaRT+ to EC", content = @Content(schema = @Schema()))
    })
    @PostMapping(
            value = "/ok",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @Trace(businessProcess = BP_RECEIPT_OK, reEnabled = true)
    public void receiptOk(@RequestBody ReceiptRequest request) throws IOException {

        receiptService.paaInviaRTOk(request.getContent());
    }

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
    public void deleteTimer(@RequestParam List<String> paymentTokens) {
        log.info("Delete Timer arrived: " + paymentTokens);
        receiptTimerService.cancelScheduledMessage(paymentTokens);

    }
}
