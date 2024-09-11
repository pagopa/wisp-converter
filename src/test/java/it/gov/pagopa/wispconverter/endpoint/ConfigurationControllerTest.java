package it.gov.pagopa.wispconverter.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wispconverter.Application;
import it.gov.pagopa.wispconverter.controller.model.ConfigurationModel;
import it.gov.pagopa.wispconverter.service.ConfigurationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ActiveProfiles(profiles = "test")
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class ConfigurationControllerTest {

    @MockBean
    private ConfigurationService configurationService;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;






    @Test
    void createCreditorInstitutionsConfiguration() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/whitelist/cis")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfigurationModel())))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    }

    @Test
    void createStationsConfiguration() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/whitelist/stations")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfigurationModel())))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    }
}