package it.gov.pagopa.wispconverter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wispconverter.config.model.AppCors;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${cors.configuration}")
    private String corsConfiguration;


    @SneakyThrows
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        AppCors appCors = new ObjectMapper().readValue(corsConfiguration,
                AppCors.class);
        registry.addMapping("/**")
                .allowedOrigins(appCors.getOrigins())
                .allowedMethods(appCors.getMethods());
    }
}


