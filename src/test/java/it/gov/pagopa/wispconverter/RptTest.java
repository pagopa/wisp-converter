package it.gov.pagopa.wispconverter;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class RptTest {

    /*
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
    @Qualifier("redisSimpleTemplate")
    @MockBean private RedisTemplate<String, Object> redisSimpleTemplate;
    @MockBean private org.openapitools.client.api.CacheApi cacheClient;

    private String getRptPayload(String station,String amount){
        String rpt = TestUtils.loadFileContent("/requests/rpt.xml");
        String rptreplace = rpt.replaceAll("\\{amount\\}", amount);
        String nodoInviaRPT = TestUtils.loadFileContent("/requests/nodoInviaRPT.xml");
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
        when(cacheClient.cache()).thenReturn(configDataV1);

        org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "cacheClient",cacheClient);

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
        when(redisSimpleTemplate.opsForValue()).thenReturn(mock(ValueOperations.class));



        mvc.perform(MockMvcRequestBuilders.get("/redirect?sessionId=aaaaaaaaaaaa").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andDo(
                        (result) -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                        });

        verify(checkoutClient,times(1)).executeCreation(any());
    }
     */
}
