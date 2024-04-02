package it.gov.pagopa.wispconverter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.service.ConverterService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

@Controller
@RequestMapping("/redirect")
@Validated
@RequiredArgsConstructor
@Tag(name = "Redirect", description = "Conversion and redirection APIs")
public class RedirectController {

    private final ConverterService converterService;

    @Operation(summary = "", description = "", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Redirect"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirect to Checkout service.", content = @Content(schema = @Schema()))
    })
    @GetMapping
    public ModelAndView redirect(@Parameter(description = "", example = "identificativoIntermediarioPA_sessionId")
                                 @NotBlank(message = "{redirect.session-id.not-blank}")
                                 @RequestParam("sessionId") String sessionId,
                                 HttpServletResponse response) throws IOException {

        String errorCode;
        try {
            response.sendRedirect(converterService.convert(sessionId));
            return null;
        } catch (AppException e) {
            errorCode = String.valueOf(e.getError().getCode());
        } catch (Exception e) {
            errorCode = String.valueOf(AppErrorCodeMessageEnum.GENERIC_ERROR.getCode());
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("error.html");
        modelAndView.addObject("error", "WIC-" + errorCode);
        return modelAndView;
    }
}
