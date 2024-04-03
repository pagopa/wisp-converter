package it.gov.pagopa.wispconverter.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import it.gov.pagopa.wispconverter.service.model.ConversionResultDTO;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ErrorUtil;
import it.gov.pagopa.wispconverter.util.MDCUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.Instant;
import java.util.Enumeration;
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

    @Operation(summary = "", description = "", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Redirect"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirect to Checkout service.", content = @Content(schema = @Schema()))
    })
    @GetMapping(value = "/redirect")
    public String redirect(@Parameter(description = "", example = "identificativoIntermediarioPA_sessionId")
                           @NotBlank(message = "{redirect.session-id.not-blank}")
                           @RequestParam("sessionId") String sessionId,
                           Model model, HttpServletResponse response) {
        try{
            ConversionResultDTO conversionResultDTO = converterService.convert(sessionId);
            return "redirect:" + conversionResultDTO.getUri();
        } catch (AppException appException){
            ErrorResponse errorResponse = errorUtil.forAppException(appException);
            ProblemDetail problemDetail = errorResponse.updateAndGetBody(this.messageSource, LocaleContextHolder.getLocale());
            errorUtil.finalizeError(problemDetail, errorResponse.getStatusCode().value());

            response.setStatus(errorResponse.getStatusCode().value());
            model.addAttribute("sessionId",sessionId);
            enrichModelWithError(model, problemDetail, errorResponse.getStatusCode().value());
            return "error";
        } catch (Exception ex) {
            String operationId = MDC.get(Constants.MDC_OPERATION_ID);
            log.error(String.format("GenericException: operation-id=[%s]", operationId!=null?operationId:"n/a"), ex);

            AppException appException = new AppException(ex, AppErrorCodeMessageEnum.ERROR, ex.getMessage());
            ErrorResponse errorResponse = errorUtil.forAppException(appException);
            ProblemDetail problemDetail = errorResponse.updateAndGetBody(this.messageSource, LocaleContextHolder.getLocale());
            errorUtil.finalizeError(problemDetail, errorResponse.getStatusCode().value());

            response.setStatus(errorResponse.getStatusCode().value());
            model.addAttribute("sessionId",sessionId);
            enrichModelWithError(model, problemDetail, errorResponse.getStatusCode().value());
            return "error";
        }

    }

    @Operation(summary = "", description = "", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Redirect"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Redirect info to Checkout service.", content = @Content(schema = @Schema(implementation = RedirectResponse.class)))
    })
    @GetMapping(value = "/redirect", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RedirectResponse> redirectInfo(@Parameter(description = "", example = "identificativoIntermediarioPA_sessionId")
                                         @NotBlank(message = "{redirect.session-id.not-blank}")
                                         @RequestParam("sessionId") String sessionId) {
        ConversionResultDTO conversionResultDTO = converterService.convert(sessionId);
        return ResponseEntity.ok(RedirectResponse.builder().redirectUrl(conversionResultDTO.getUri()).build());
    }

    private void enrichModelWithError(Model model, ProblemDetail problemDetail, int statusCode){
        model.addAttribute("type",problemDetail.getType());
        model.addAttribute("title",problemDetail.getTitle());
        model.addAttribute("status",statusCode);
        model.addAttribute("detail",problemDetail.getDetail());

        Map<String, Object> properties = problemDetail.getProperties();
        if(properties!=null){
            Instant timestamp = (Instant) properties.get(ErrorUtil.EXTRA_FIELD_ERROR_TIMESTAMP);
            if(timestamp!=null){
                model.addAttribute("timestamp",timestamp.toString());
            }

            String errrCode = (String) properties.get(ErrorUtil.EXTRA_FIELD_ERROR_CODE);
            if(errrCode!=null){
                model.addAttribute("errorCode",errrCode);
            }

            String operationId = (String) properties.get(ErrorUtil.EXTRA_FIELD_OPERATION_ID);
            if(operationId!=null){
                model.addAttribute("operationId",operationId);
            }
        }
    }



//    @RequestMapping("/redirect-error")
//    public String redirectError(Model model, HttpServletRequest request) throws IOException {
//        model.addAttribute("summary","1 2 3");
//        return "error";
//    }
}
