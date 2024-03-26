package it.gov.pagopa.wispconverter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.gov.pagopa.wispconverter.model.converter.ConversionResult;
import it.gov.pagopa.wispconverter.service.ConverterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redirect")
@Validated
public class RedirectController {

    private final ConverterService converterService;

    public RedirectController(@Autowired ConverterService converterService) {
        this.converterService = converterService;
    }

    public static ResponseEntity<Object> generateConversionResponse(ConversionResult conversionResult) {
        ResponseEntity<Object> result;
        if (conversionResult.isSuccess()) {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setLocation(conversionResult.getUri());
            result = new ResponseEntity<>(httpHeaders, HttpStatus.FOUND);
        } else {
            result = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(conversionResult.getErrorPage());
        }
        return result;
    }

    @Operation(summary = "", description = "", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Home"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirect to Checkout service.", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "401", description = "Wrong or missing function key.", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(mediaType = MediaType.TEXT_HTML_VALUE))})
    @GetMapping
    public ResponseEntity<Object> redirect(@Parameter(description = "", example = "identificativoIntermediarioPA_sessionId") @RequestParam("sessionId") String sessionId) {
        return generateConversionResponse(converterService.convert(sessionId));
    }
}
