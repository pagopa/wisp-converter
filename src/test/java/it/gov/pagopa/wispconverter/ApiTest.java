package it.gov.pagopa.standinmanager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import it.gov.pagopa.wispconverter.Application;
import it.gov.pagopa.wispconverter.client.cache.model.ConfigDataV1;
import it.gov.pagopa.wispconverter.client.cache.model.Redirect;
import it.gov.pagopa.wispconverter.client.cache.model.Station;
import it.gov.pagopa.wispconverter.client.checkout.CheckoutClient;
import it.gov.pagopa.wispconverter.client.decoupler.DecouplerCachingClient;
import it.gov.pagopa.wispconverter.client.gpd.GPDClient;
import it.gov.pagopa.wispconverter.client.iuvgenerator.IUVGeneratorClient;
import it.gov.pagopa.wispconverter.client.iuvgenerator.model.IUVGeneratorResponse;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.service.ConfigCacheService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class ApiTest {

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

    public String loadFileContent(String fileName) {
        String content = null;
        try {
            // Get the InputStream of the resource
            InputStream inputStream = this.getClass().getResourceAsStream(fileName);
            if (inputStream != null) {
                // Use Apache Commons IO to read the content from the InputStream
                content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            } else {
                System.err.println("File not found: " + fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    private String getRptPayload(String station,String amount){
        String rpt = loadFileContent("/requests/rpt.xml");
        String rptreplace = rpt.replace("{amount}", amount);
        String nodoInviaRPT = loadFileContent("/requests/nodoInviaRPT.xml");
        return nodoInviaRPT
                .replace("{station}",station)
                .replace("{rpt}", Base64.getEncoder().encodeToString(rptreplace.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] zip(byte[] uncompressed) throws IOException {
        ByteArrayOutputStream bais = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bais);
        gzipOutputStream.write(uncompressed);
        gzipOutputStream.close();
        bais.close();
        return bais.toByteArray();
    }

    @Test
    void success() throws Exception {
        ConfigDataV1 configDataV1 = new ConfigDataV1();
        configDataV1.setStations(new HashMap<>());
        Station station = new Station();
        station.setStationCode("mystation");
        station.setRedirect(new Redirect());
        station.getRedirect().setIp("127.0.0.1");
        station.getRedirect().setPath("/redirect");
        station.getRedirect().setPort(8888l);
        station.getRedirect().setProtocol(Redirect.ProtocolEnum.HTTPS);
        station.getRedirect().setQueryString("param=1");
        configDataV1.getStations().put(station.getStationCode(), station);
        org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "configData",configDataV1);

        HashMap<String, Collection<String>> headers = new HashMap<>();
        headers.put("location", Arrays.asList("locationheader"));
    Response executeCreationResponse =
        Response.builder()
            .status(302)
            .headers(headers)
            .request(Request.create(Request.HttpMethod.GET, "", new HashMap<>(),"".getBytes(StandardCharsets.UTF_8),null))
            .build();

        when(checkoutClient.executeCreation(any())).thenReturn(executeCreationResponse);

        when(iuveneratorClient.generate(any(),any())).thenReturn(
                IUVGeneratorResponse.builder().iuv("00000000").build()
        );
        when(rptRequestRepository.findById(any())).thenReturn(
                Optional.of(
                        RPTRequestEntity.builder().primitive("nodoInviaRPT")
                                .payload(
                                        new String(Base64.getEncoder().encode(zip(
                                                getRptPayload(station.getStationCode(),
                                                        "100.00"
                                                ).getBytes(StandardCharsets.UTF_8))),StandardCharsets.UTF_8)
                                ).build()
                )
        );
        mvc.perform(MockMvcRequestBuilders.get("/redirect?sessionId=aaaaaaaaaaaa").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andDo(
                        (result) -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                        });

        verify(checkoutClient,times(1)).executeCreation(any());
    }
}
