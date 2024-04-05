package it.gov.pagopa.wispconverter.util.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

@Component
public class OpenAPIDescriptionCustomizer implements OpenApiCustomizer {


    private static final String SEPARATOR = " | ";

    private static String buildErrorData() {
        StringBuilder builder = new StringBuilder("\n\n**STANDARD ERRORS:**\n");
        builder.append("NAME").append(SEPARATOR).append("CODE").append(SEPARATOR).append("DESCRIPTION").append("\n");
        builder.append("-").append(SEPARATOR).append("-").append(SEPARATOR).append("-").append("\n");

        for (AppErrorCodeMessageEnum errorCode : AppErrorCodeMessageEnum.values()) {

            String detail = errorCode.getDetail();
            detail = detail.replaceAll("(\\[\\{\\d+\\}\\])", " [*...content...*]");
            detail = detail.replaceAll("(\\{\\d+\\})", " *...error description...*");

            builder.append("**").append(CommonUtility.getAppCode(errorCode)).append("**").append(SEPARATOR)
                    .append("*").append(errorCode.name()).append("*").append(SEPARATOR)
                    .append(detail).append("\n");
        }

        return builder.toString();
    }

    @Override
    public void customise(OpenAPI openApi) {
        Info info = openApi.getInfo();
        info.setDescription(info.getDescription() + buildErrorData());
    }
}