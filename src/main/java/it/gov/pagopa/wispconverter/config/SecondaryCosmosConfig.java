package it.gov.pagopa.wispconverter.config;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.spring.data.cosmos.config.AbstractCosmosConfiguration;
import com.azure.spring.data.cosmos.config.CosmosConfig;
import com.azure.spring.data.cosmos.core.CosmosTemplate;
import com.azure.spring.data.cosmos.core.convert.MappingCosmosConverter;
import com.azure.spring.data.cosmos.repository.config.EnableCosmosRepositories;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableCosmosRepositories(basePackages = "it.gov.pagopa.wispconverter.repository.secondary")
@EnableConfigurationProperties
@ConditionalOnExpression("'${info.properties.environment}'!='test'")
@Slf4j
public class SecondaryCosmosConfig extends AbstractCosmosConfiguration {

    @Value("${azure.cosmos.uri}")
    private String cosmosUri;

    @Value("${azure.cosmos.key}")
    private String cosmosKey;

    @Value("${azure.cosmos.database}")
    private String cosmosDatabase;

    @Value("${azure.cosmos.read.region}")
    private String readRegion;

    @Value("${azure.cosmos.populate-query-metrics}")
    private Boolean cosmosQueryMetrics;

    @Bean(name = "secondaryCosmosClient")
    public CosmosAsyncClient getCosmosAsyncClient() {
        return new CosmosClientBuilder()
                .key(cosmosKey)
                .endpoint(cosmosUri)
                .preferredRegions(List.of(readRegion))
                .buildAsyncClient();
    }

    @Bean(name = "secondaryCosmosTemplate")
    public CosmosTemplate secondaryCosmosTemplate(@Qualifier("secondaryCosmosClient") CosmosAsyncClient client,
                                                  MappingCosmosConverter mappingCosmosConverter) {
        return new CosmosTemplate(client, cosmosDatabase, cosmosConfig(), mappingCosmosConverter);
    }

    @Override
    public CosmosConfig cosmosConfig() {
        return CosmosConfig.builder()
                .enableQueryMetrics(cosmosQueryMetrics)
                .build();
    }

    @Override
    protected String getDatabaseName() {
        return cosmosDatabase;
    }
}
