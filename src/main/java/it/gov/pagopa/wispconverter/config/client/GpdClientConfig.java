package it.gov.pagopa.wispconverter.config.client;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.gov.pagopa.wispconverter.service.ReService;
import it.gov.pagopa.wispconverter.util.client.RequestResponseLoggingProperties;
import it.gov.pagopa.wispconverter.util.client.gpd.GpdClientLoggingInterceptor;
import it.gov.pagopa.wispconverter.util.client.gpd.GpdClientResponseErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class GpdClientConfig {

    private final ReService reService;

    @Value("${client.gpd.read-timeout}")
    private Integer readTimeout;

    @Value("${client.gpd.connect-timeout}")
    private Integer connectTimeout;

    @Value("${client.gpd.base-path}")
    private String basePath;

    @Value("${client.gpd.api-key}")
    private String apiKey;

    @Value("${wisp-converter.re-tracing.interface.payment-position-analysis.enabled}")
    private Boolean isTracingOfClientOnREEnabled;

    @Bean
    @ConfigurationProperties(prefix = "log.client.gpd")
    public RequestResponseLoggingProperties gpdClientLoggingProperties() {
        return new RequestResponseLoggingProperties();
    }

    @Bean
    public it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient gpdClient() {
        RequestResponseLoggingProperties clientLoggingProperties = gpdClientLoggingProperties();

        GpdClientLoggingInterceptor clientLogging = new GpdClientLoggingInterceptor(clientLoggingProperties, reService, isTracingOfClientOnREEnabled);

        RestTemplate restTemplate = restTemplate();

        List<ClientHttpRequestInterceptor> currentInterceptors = restTemplate.getInterceptors();
        currentInterceptors.add(clientLogging);
        restTemplate.setInterceptors(currentInterceptors);

        restTemplate.setErrorHandler(new GpdClientResponseErrorHandler());

        it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient client = new it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient(restTemplate);

        client.setBasePath(basePath);
        client.setApiKey(apiKey);

        return client;
    }

    private RestTemplate restTemplate() {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        SimpleModule module = new SimpleModule();
        /*
        module.addSerializer(LocalDateTime.class, new JsonSerializer<>() {
            @Override
            public void serialize(LocalDateTime date, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
                String formattedDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                jsonGenerator.writeString(formattedDate);
            }
        });
        module.addDeserializer(LocalDateTime.class, new JsonDeserializer<>() {
            @Override
            public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                String date = parser.getText();
                return LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        });
        */
        module.addSerializer(OffsetDateTime.class, new JsonSerializer<>() {
            @Override
            public void serialize(OffsetDateTime date, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
                LocalDateTime localDateTime = date.toLocalDateTime();
                String formattedDate = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                jsonGenerator.writeString(formattedDate);
            }
        });
        module.addDeserializer(OffsetDateTime.class, new JsonDeserializer<>() {
            @Override
            public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                String date = parser.getText();
                return LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atOffset(ZoneOffset.UTC);
            }
        });
        objectMapper.registerModule(module);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);

        RestTemplate restTemplate = new RestTemplateBuilder().messageConverters(converter).build();

        // This allows us to read the response more than once - Necessary for debugging.
        restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(getSimpleClientHttpRequestFactory(restTemplate.getRequestFactory())));

        // disable default URL encoding
        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
        uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
        restTemplate.setUriTemplateHandler(uriBuilderFactory);
        return restTemplate;
    }

    private SimpleClientHttpRequestFactory getSimpleClientHttpRequestFactory(ClientHttpRequestFactory requestFactory) {
        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = (SimpleClientHttpRequestFactory) requestFactory;
        simpleClientHttpRequestFactory.setConnectTimeout(this.connectTimeout);
        simpleClientHttpRequestFactory.setReadTimeout(this.readTimeout);
        return simpleClientHttpRequestFactory;
    }
}