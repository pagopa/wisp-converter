package it.gov.pagopa.wispconverter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wispconverter.config.model.AppCors;
import it.gov.pagopa.wispconverter.util.MDCEnrichInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    List<Locale> locales = Arrays.asList(Locale.ENGLISH, Locale.ITALIAN);

    @Value("${cors.configuration}")
    private String corsConfiguration;

    @Value("${filter.exclude-url-patterns}")
    private List<String> excludeUrlPatterns;


    @SneakyThrows
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        AppCors appCors = new ObjectMapper().readValue(corsConfiguration, AppCors.class);
        registry.addMapping("/**")
                .allowedOrigins(appCors.getOrigins())
                .allowedMethods(appCors.getMethods());
    }


    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver acceptHeaderLocaleResolver = new AcceptHeaderLocaleResolver();
        acceptHeaderLocaleResolver.setDefaultLocale(Locale.ENGLISH);
        acceptHeaderLocaleResolver.setSupportedLocales(locales);
        return acceptHeaderLocaleResolver;
    }


    @Bean
    public ResourceBundleMessageSource messageSource() {
        var resourceBundleMessageSource=new ResourceBundleMessageSource();
        resourceBundleMessageSource.setBasename("i18n/messages"); // directory with messages_XX.properties
        resourceBundleMessageSource.setDefaultLocale(Locale.ENGLISH);
        resourceBundleMessageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        resourceBundleMessageSource.setAlwaysUseMessageFormat(true);
        return resourceBundleMessageSource;
    }

    @Primary
    @Bean
    @Override
    public LocalValidatorFactoryBean getValidator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource());
        return bean;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MDCEnrichInterceptor()).excludePathPatterns(excludeUrlPatterns);
    }
}


