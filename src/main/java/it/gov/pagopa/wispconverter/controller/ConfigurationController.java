package it.gov.pagopa.wispconverter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.wispconverter.controller.model.ConfigurationModel;
import it.gov.pagopa.wispconverter.service.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whitelist")
@Validated
@Tag(name = "Configuration", description = "ECs and Stations configuration")
public class ConfigurationController {

    private final ConfigurationService configurationService;

    @Autowired
    public ConfigurationController(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }


    /**
     * Configuration for creditor institutions
     *
     * @return creditor institution configuration string
     */
    @Operation(summary = "Return the string containing all creditor institutions for the wisp converter logic", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Configuration"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration for EC retrieved.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ConfigurationModel.class))),
            @ApiResponse(responseCode = "404", description = "Configuration for EC not found."),
    })
    @GetMapping("/cis")
    public ConfigurationModel getCreditorInstitutions() {
        return ConfigurationModel.builder()
                .key(configurationService.getCreditorInstitutionConfiguration())
                .build();
    }

    /**
     * Configuration for station
     *
     * @return station configuration string
     */
    @Operation(summary = "Return the string containing all stations for the wisp converter logic", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Configuration"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration for Stations retrieved.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ConfigurationModel.class))),
            @ApiResponse(responseCode = "404", description = "Configuration for Stations not found."),
    })
    @GetMapping("/stations")
    public ConfigurationModel getStations() {
        return ConfigurationModel.builder()
                .key(configurationService.getStationConfiguration())
                .build();
    }


    @Operation(summary = "Create the string containing all creditor institutions for the wisp converter logic", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Configuration"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK."),
    })
    @PostMapping(value = "/cis")
    @ResponseStatus(HttpStatus.CREATED)
    public void createCreditorInstitutionsConfiguration(@RequestBody ConfigurationModel body) {
        configurationService.createCreditorInstitutionsConfiguration(body);
    }

    @Operation(summary = "Create the string containing all stations for the wisp converter logic", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Configuration"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK."),
    })
    @PostMapping(value = "/stations")
    @ResponseStatus(HttpStatus.CREATED)
    public void createStationsConfiguration(@RequestBody ConfigurationModel body) {
        configurationService.createStationsConfiguration(body);
    }

}
