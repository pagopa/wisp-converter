package it.gov.pagopa.wispconverter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import it.gov.pagopa.wispconverter.config.model.AppCors;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.jackson.JsonMixinModule;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;


@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    List<Locale> locales = Arrays.asList(Locale.ENGLISH, Locale.ITALIAN);

    @Value("${cors.configuration}")
    private String corsConfiguration;

    private static final String dateTimeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'";

//    @Bean
//    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
//        return builder -> {
//            builder.mixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
//            builder.simpleDateFormat(dateTimeFormat);
//            builder.serializers(new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(dateTimeFormat)));
//        };
//    }


//    @Autowired
//    private ObjectMapper objectMapper;
//    @Bean
//    public ObjectMapper objectMapper(){
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
//        return objectMapper;
//    }
//    @Bean
//    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(){
//        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter(objectMapper);
//        return mappingJackson2HttpMessageConverter;
//    }

//    @Bean
//    public Jackson2ObjectMapperBuilder jacksonBuilder() {
//        Jackson2ObjectMapperBuilder b = new Jackson2ObjectMapperBuilder();
//        b.indentOutput(true).dateFormat(new SimpleDateFormat("yyyy-MM-dd"));
//        b.mixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
//        return b;
//    }



    @SneakyThrows
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        AppCors appCors = new ObjectMapper().readValue(corsConfiguration,
                AppCors.class);
        registry.addMapping("/**")
                .allowedOrigins(appCors.getOrigins())
                .allowedMethods(appCors.getMethods());
    }

//    @Bean
//    public Jackson2ObjectMapperBuilder jacksonBuilder() {
//        Jackson2ObjectMapperBuilder b = new Jackson2ObjectMapperBuilder();
//        b.indentOutput(true).mixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
//        return b;
//    }


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
        resourceBundleMessageSource.setBasenames("i18n/messages"); // directory with messages_XX.properties
        resourceBundleMessageSource.setUseCodeAsDefaultMessage(true);
//        resourceBundleMessageSource.setDefaultLocale(Locale.of("en"));
        resourceBundleMessageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        resourceBundleMessageSource.setAlwaysUseMessageFormat(true);
        return resourceBundleMessageSource;
    }


//    @Primary
//    @Bean
//    @Override
//    public LocalValidatorFactoryBean getValidator() {
//        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
//        bean.setValidationMessageSource(messageSource());
//        return bean;
//    }
}


