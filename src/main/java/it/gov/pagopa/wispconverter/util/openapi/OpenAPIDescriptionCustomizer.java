package it.gov.pagopa.wispconverter.util.openapi;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.util.CommonUtility;
import lombok.SneakyThrows;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class OpenAPIDescriptionCustomizer implements OpenApiCustomizer {


    private static final String SEPARATOR = " | ";

    @SneakyThrows
    private static String buildAdditionalInfo() {
        StringBuilder builder = new StringBuilder("\n\n# OPERATIVE INFO\n");

        // setting information about Registro Eventi
        builder.append("\n\n## EVENT MAPPING IN RE\n");
        builder.append("\n<details><summary>Details</summary>\n");
        generateEventMappingInRESection(builder);
        builder.append("\n</details>\n");

        // setting information about standard errors retrievable from Registro Eventi
        builder.append("\n\n## OPERATIONAL ERROR CODES\n");
        builder.append("\n<details><summary>Details</summary>\n");
        generateOperationalErrorCodeSection(builder);

        builder.append("\n</details>\n");
        return builder.toString();
    }

    private static void generateEventMappingInRESection(StringBuilder builder) throws NoSuchFieldException {
        builder.append("FIELD").append(SEPARATOR).append("SCOPE").append(SEPARATOR).append("DESCRIPTION").append("\n");
        builder.append("-").append(SEPARATOR).append("-").append(SEPARATOR).append("-").append("\n");
        for (Field field : ReEventDto.class.getDeclaredFields()) {
            Schema schema = field.getAnnotation(Schema.class);
            if (schema != null) {
                StringBuilder description = new StringBuilder(schema.description());
                Class<?> fieldClass = field.getType();
                if (fieldClass.isEnum()) {
                    description.append("<br>Values: ");
                    for (Object enumConstant : fieldClass.getEnumConstants()) {
                        Field constantField = fieldClass.getField(enumConstant.toString());
                        description.append("<br>_").append(enumConstant).append("_");
                        Schema s = constantField.getAnnotation(Schema.class);
                        if (s != null) {
                            description.append(": ").append(s.description());
                        }
                    }
                }
                builder.append("**").append(field.getName()).append("**").append(SEPARATOR)
                        .append(schema.title()).append(SEPARATOR)
                        .append(description).append("\n");
            }
        }
    }

    private static void generateOperationalErrorCodeSection(StringBuilder builder) {
        builder.append("NAME").append(SEPARATOR).append("CODE").append(SEPARATOR).append("DESCRIPTION").append("\n");
        builder.append("-").append(SEPARATOR).append("-").append(SEPARATOR).append("-").append("\n");

        for (AppErrorCodeMessageEnum errorCode : AppErrorCodeMessageEnum.values()) {

            String detail = errorCode.getOpenapiDescription();
            builder.append("**").append(CommonUtility.getAppCode(errorCode)).append("**").append(SEPARATOR)
                    .append("*").append(errorCode.name()).append("*").append(SEPARATOR)
                    .append(detail).append("\n");
        }
    }

    @Override
    public void customise(OpenAPI openApi) {
        Info info = openApi.getInfo();
        info.setDescription(info.getDescription() + buildAdditionalInfo());
    }
}