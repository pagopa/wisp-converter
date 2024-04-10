package it.gov.pagopa.wispconverter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.wispconverter.controller.model.RedirectResponse;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.service.ConverterService;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ErrorUtil;
import it.gov.pagopa.wispconverter.util.Trace;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@Controller
@RequestMapping
@Validated
@RequiredArgsConstructor
@Tag(name = "Redirect", description = "Conversion and redirection APIs")
@Slf4j
public class RedirectController {

    private final ConverterService converterService;
    private final ErrorUtil errorUtil;
    private final MessageSource messageSource;

    private static final String BP_REDIRECT = "redirect";

    @Operation(summary = "", description = "", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Redirect"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirect to Checkout service.", content = @Content(schema = @Schema()))
    })
    @GetMapping(value = "/redirect")
    @Trace(businessProcess=BP_REDIRECT, reEnabled = true)
    public String redirect(@Parameter(description = "", example = "identificativoIntermediarioPA_sessionId")
                           @NotBlank(message = "{redirect.session-id.not-blank}")
                           @RequestParam("sessionId") String sessionId,
                           Model model, HttpServletResponse response) {
        try {
            String redirectURI = converterService.convert(sessionId);
            return "redirect:" + redirectURI;
        } catch (AppException appException) {
            ErrorResponse errorResponse = errorUtil.forAppException(appException);
            ProblemDetail problemDetail = errorResponse.updateAndGetBody(this.messageSource, LocaleContextHolder.getLocale());
            errorUtil.finalizeError(problemDetail, errorResponse.getStatusCode().value());

            response.setStatus(errorResponse.getStatusCode().value());
            model.addAttribute("sessionId", sessionId);
            enrichModelWithError(model, problemDetail, errorResponse.getStatusCode().value());
            return "error";
        } catch (Exception ex) {
            String operationId = MDC.get(Constants.MDC_OPERATION_ID);
            log.error(String.format("GenericException: operation-id=[%s]", operationId != null ? operationId : "n/a"), ex);

            AppException appException = new AppException(ex, AppErrorCodeMessageEnum.ERROR, ex.getMessage());
            ErrorResponse errorResponse = errorUtil.forAppException(appException);
            ProblemDetail problemDetail = errorResponse.updateAndGetBody(this.messageSource, LocaleContextHolder.getLocale());
            errorUtil.finalizeError(problemDetail, errorResponse.getStatusCode().value());

            response.setStatus(errorResponse.getStatusCode().value());
            model.addAttribute("sessionId", sessionId);
            enrichModelWithError(model, problemDetail, errorResponse.getStatusCode().value());
            return "error";
        }

    }


    @Operation(summary = "", description = "", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Redirect"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Redirect info to Checkout service.", content = @Content(schema = @Schema(implementation = RedirectResponse.class)))
    })
    @GetMapping(value = "/redirect", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Trace(businessProcess=BP_REDIRECT, reEnabled = true)
    public ResponseEntity<RedirectResponse> redirectInfo(@Parameter(description = "", example = "identificativoIntermediarioPA_sessionId")
                                                         @NotBlank(message = "{redirect.session-id.not-blank}")
                                                         @RequestParam("sessionId") String sessionId) {
        String redirectURI = converterService.convert(sessionId);
        return ResponseEntity.ok(RedirectResponse.builder().redirectUrl(redirectURI).build());
    }

    private void enrichModelWithError(Model model, ProblemDetail problemDetail, int statusCode) {
        model.addAttribute("type", problemDetail.getType());
        model.addAttribute("title", problemDetail.getTitle());
        model.addAttribute("status", statusCode);
        model.addAttribute("detail", problemDetail.getDetail());

        Map<String, Object> properties = problemDetail.getProperties();
        if (properties != null) {
            Instant timestamp = (Instant) properties.get(ErrorUtil.EXTRA_FIELD_ERROR_TIMESTAMP);
            if (timestamp != null) {
                model.addAttribute("timestamp", timestamp.toString());
            }

            String errrCode = (String) properties.get(ErrorUtil.EXTRA_FIELD_ERROR_CODE);
            if (errrCode != null) {
                model.addAttribute("errorCode", errrCode);
            }

            String operationId = (String) properties.get(ErrorUtil.EXTRA_FIELD_OPERATION_ID);
            if (operationId != null) {
                model.addAttribute("operationId", operationId);
            }
        }
    }

}
