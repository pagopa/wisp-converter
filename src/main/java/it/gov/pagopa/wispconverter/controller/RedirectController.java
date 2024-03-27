package it.gov.pagopa.wispconverter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.gov.pagopa.wispconverter.service.ConverterService;
import it.gov.pagopa.wispconverter.service.model.ConversionResultDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/redirect")
@Validated
@RequiredArgsConstructor
public class RedirectController {

    private final ConverterService converterService;

    /*
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
     */

    @Operation(summary = "", description = "", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Home"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirect to Checkout service.", content = @Content(schema = @Schema())),
            //@ApiResponse(responseCode = "401", description = "Wrong or missing function key.", content = @Content(schema = @Schema())),
            //@ApiResponse(responseCode = "403", description = "Forbidden.", content = @Content(schema = @Schema())),
            //@ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(mediaType = MediaType.TEXT_HTML_VALUE))
    })
    @GetMapping
    public void redirect(@Parameter(description = "", example = "identificativoIntermediarioPA_sessionId") @RequestParam("sessionId") String sessionId,
                         HttpServletResponse response) throws IOException {
        ConversionResultDTO conversionResultDTO = converterService.convert(sessionId);
        response.sendRedirect(conversionResultDTO.getUri());
    }
}
