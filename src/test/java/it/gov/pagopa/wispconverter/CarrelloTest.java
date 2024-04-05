package it.gov.pagopa.wispconverter;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class CarrelloTest {

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

    private String getCarrelloPayload(int numofrpt,String station,String amount,boolean multibeneficiario){
        String rpt = TestUtils.loadFileContent("/requests/rpt.xml");
        String rptreplace = rpt.replaceAll("\\{amount\\}", amount);
        StringBuilder listaRpt = new StringBuilder("");
        for(int i=0;i<numofrpt;i++){
            listaRpt.append(
                    ("<elementoListaRPT>"+
                    "<identificativoDominio></identificativoDominio>"+
                    "<identificativoUnivocoVersamento></identificativoUnivocoVersamento>"+
                    "<codiceContestoPagamento></codiceContestoPagamento>"+
                    "<tipoFirma></tipoFirma>"+
                    "<rpt>{rpt}</rpt>" +
                    "</elementoListaRPT>").replace("{rpt}",Base64.getEncoder().encodeToString(rptreplace.getBytes(StandardCharsets.UTF_8)))
            );
        }

        String carrello = TestUtils.loadFileContent("/requests/nodoInviaCarrelloRPT.xml");
        return carrello
                .replace("{station}",station)
                .replace("{multi}",multibeneficiario?"<multiBeneficiario>true</multiBeneficiario>":"")
                .replace("{elementiRpt}", listaRpt.toString());
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
        ConfigDataV1Dto configDataV1 = new ConfigDataV1Dto();
        configDataV1.setStations(new HashMap<>());
        StationDto station = new StationDto();
        station.setStationCode("mystation");
        station.setRedirect(new RedirectDto());
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
                        RPTRequestEntity.builder().primitive("nodoInviaCarrelloRPT")
                                .payload(
                                        new String(Base64.getEncoder().encode(zip(
                                                getCarrelloPayload(1,station.getStationCode(),
                                                        "100.00",false
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

    @Test
    void success_multibeneficiario() throws Exception {
        ConfigDataV1Dto configDataV1 = new ConfigDataV1Dto();
        configDataV1.setStations(new HashMap<>());
        StationDto station = new StationDto();
        station.setStationCode("mystation");
        station.setRedirect(new RedirectDto());
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
                        RPTRequestEntity.builder().primitive("nodoInviaCarrelloRPT")
                                .payload(
                                        new String(Base64.getEncoder().encode(zip(
                                                getCarrelloPayload(2,station.getStationCode(),
                                                        "100.00",true
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
