package it.gov.pagopa.wispconverter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wispconverter.client.checkout.CheckoutClient;
import it.gov.pagopa.wispconverter.client.decoupler.DecouplerCachingClient;
import it.gov.pagopa.wispconverter.client.gpd.GPDClient;
import it.gov.pagopa.wispconverter.client.iuvgenerator.IUVGeneratorClient;
import it.gov.pagopa.wispconverter.controller.model.AppInfoResponse;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.service.ConfigCacheService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class HomeTest {

    @Value("${info.application.name}")
    private String name;

    @Value("${info.application.version}")
    private String version;

    @Value("${info.properties.environment}")
    private String environment;

    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    private MockMvc mvc;
    @Autowired private ConfigCacheService configCacheService;

//    @MockBean private CosmosStationRepository cosmosStationRepository;
//    @MockBean private CosmosEventsRepository cosmosEventsRepository;
//    @MockBean private DatabaseStationsRepository databaseStationsRepository;
//    @MockBean private EntityManagerFactory entityManagerFactory;
//    @MockBean private EntityManager entityManager;
//    @MockBean private DataSource dataSource;
//    @MockBean private CosmosClient cosmosClient;
    @MockBean private RPTRequestRepository rptRequestRepository;
    @MockBean private IUVGeneratorClient iuveneratorClient;
    @MockBean private GPDClient gpdClient;
    @MockBean private CheckoutClient checkoutClient;
    @MockBean private DecouplerCachingClient decouplerCachingClient;
    @MockBean private CacheRepository cacheRepository;

    @Test
    void slash() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());

    }

    @Test
    void info() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/info").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful()).andDo(
                        (result) -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                            final String content = result.getResponse().getContentAsString();
                            assertFalse(content.isBlank());
                            assertFalse(content.contains("${"), "Generated swagger contains placeholders");
                            AppInfoResponse info = objectMapper.readValue(result.getResponse().getContentAsString(), AppInfoResponse.class);
                            assertEquals(info.getName(),name);
                            assertEquals(info.getEnvironment(),environment);
                            assertEquals(info.getVersion(),version);
                        });

    }
}
