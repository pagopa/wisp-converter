package it.gov.pagopa.wispconverter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.wispconverter.controller.model.ConfigurationResponse;
import it.gov.pagopa.wispconverter.service.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/whitelist")
@Validated
@Tag(name = "Configuration", description = "ECs and Stations configuration")
public class ConfigurationController {

    @Autowired
    ConfigurationService configurationService;


    /**
     * Configuration for creditor institutions
     *
     * @return ok
     */
    @Operation(summary = "Return the string containing all creditor institutions for the wisp converter logic", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Configuration"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ConfigurationResponse.class))),
    })
    @GetMapping("/cis")
    public ConfigurationResponse getCreditorInstitutions() {
        return ConfigurationResponse.builder()
                .key(configurationService.getCreditorInstitutionConfiguration())
                .build();
    }

}
