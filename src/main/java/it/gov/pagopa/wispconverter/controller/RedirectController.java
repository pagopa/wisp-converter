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
import it.gov.pagopa.wispconverter.util.CommonUtility;
import it.gov.pagopa.wispconverter.util.openapi.OpenAPITableMetadata;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
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

    @Operation(summary = "Redirect payment from WISP to Checkout", description = "Redirecting the payment flow from WISP to Checkout. In order to do so, the NodoInviaRPT and NodoInviaCarrelloRPT requests will be converted in NMU payments and handled by GPD system.", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Redirect"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirect to Checkout service.", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "422", description = "Unprocessable request. Returns a static page with the error code.", content = @Content(schema = @Schema()))
    })
    @OpenAPITableMetadata(external = false, authentication = OpenAPITableMetadata.APISecurityMode.APIKEY, readWriteIntense = OpenAPITableMetadata.ReadWrite.BOTH)
    @GetMapping
    public ModelAndView redirect(@Parameter(description = "The identifier of the payment, referenced by session and creditor institution broker.", example = "identificativoIntermediarioPA_sessionId")
                                 @NotBlank @RequestParam("sessionId") String sessionId,
                                 HttpServletResponse response) throws IOException {

        AppErrorCodeMessageEnum errorCode;
        try {
            response.sendRedirect(converterService.convert(sessionId));
            return null;
        } catch (AppException e) {
            errorCode = e.getError();
        } catch (Exception e) {
            errorCode = AppErrorCodeMessageEnum.GENERIC_ERROR;
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setStatus(HttpStatusCode.valueOf(422));
        modelAndView.setViewName("error.html");
        modelAndView.addObject("error", CommonUtility.getAppCode(errorCode));
        return modelAndView;
    }
}
