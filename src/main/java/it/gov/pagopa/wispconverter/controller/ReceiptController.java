//package it.gov.pagopa.wispconverter.controller;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.media.Content;
//import io.swagger.v3.oas.annotations.media.Schema;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import it.gov.pagopa.wispconverter.controller.model.ReceiptRequest;
//import it.gov.pagopa.wispconverter.service.ReceiptService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.MediaType;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.*;
//
//import java.io.IOException;
//
//@RestController
//@RequestMapping("/receipt")
//@Validated
//@RequiredArgsConstructor
//@Tag(name = "Receipt", description = "Convert sendPaymentResultV2, closePaymentV2 or paSendRTV2 into paaInviaRT to EC")
//public class ReceiptController {
//
//    private final ReceiptService receiptService;
//
//    @Operation(summary = "", description = "", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Receipt"})
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Successfully forwarded paaInviaRT- to EC", content = @Content(schema = @Schema()))
//    })
//    @PostMapping(
//            value = "/ko",
//            produces = MediaType.APPLICATION_JSON_VALUE,
//            consumes = MediaType.APPLICATION_JSON_VALUE
//    )
//    public void receiptKo(@RequestBody ReceiptRequest request) throws IOException {
//
//        receiptService.paaInviaRTKo(request.getContent());
//    }
//
//    @Operation(summary = "", description = "", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Receipt"})
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Successfully forwarded paaInviaRT+ to EC", content = @Content(schema = @Schema()))
//    })
//    @PostMapping(
//            value = "/ok",
//            produces = MediaType.APPLICATION_JSON_VALUE,
//            consumes = MediaType.APPLICATION_JSON_VALUE
//    )
//    public void receiptOk(@RequestBody ReceiptRequest request) throws IOException {
//
//        receiptService.paaInviaRTOk(request.getContent());
//    }
//}
