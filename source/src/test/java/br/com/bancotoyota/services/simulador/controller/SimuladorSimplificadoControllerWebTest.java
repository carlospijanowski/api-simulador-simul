package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.request.DadosContatoRequest;
import br.com.bancotoyota.services.simulador.beans.request.FatorSimulacao;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoSimplificadaDesejadaRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoSimplificadaResidualRequest;
import br.com.bancotoyota.services.simulador.beans.request.TextoAvisoLegalRequest;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.services.CalculadoraSeguroFranquiaServices;
import br.com.bancotoyota.services.simulador.services.CalculadoraSeguroMecanicaServices;
import br.com.bancotoyota.services.simulador.services.CalculadoraServices;
import br.com.bancotoyota.services.simulador.services.DataCarencia;
import br.com.bancotoyota.services.simulador.services.RestaUmService;
import br.com.bancotoyota.services.simulador.services.exceptions.HttpStatusInvalidException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {
        SimuladorSimplificadoController.class,
        SimuladorParcDesejadaController.class,
        CalculadoraServices.class,
        DataCarencia.class,
        RestaUmService.class,
        SimuladorParcResidualController.class,
        SimuladorParcDesejadaService.class
})
public class SimuladorSimplificadoControllerWebTest extends SimuladorSimplificadoControllerBaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FeatureToggleConfig featureToggleConfig;

    @MockBean
    private FatorSimulacao fatorSimulacao;

    @MockBean
    private CalculadoraSeguroFranquiaServices seguroFranquiaServices;

    @MockBean
    private CalculadoraSeguroMecanicaServices seguroGarantiaServices;

    @Before
    public void setup() throws HttpStatusInvalidException {
        super.setup();
    }

    @Test
    public void shouldSimuladorParcelaDesejadaResponseOK() throws Exception {
        SimulacaoSimplificadaDesejadaRequest request = criarSimulacaoSimplificadaDesejadaRequest(BigDecimal.valueOf(1000));
        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/desejada/simulacao/simplificada")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request));
        mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void shouldSimuladorParcelaResidualResponseOK() throws Exception {
        SimulacaoSimplificadaResidualRequest request = criarSimulacaoSimplificadaResidualRequest(PRAZOS, BigDecimal.valueOf(10000));
        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/residual/simulacao/simplificada")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request));
        mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void shouldBuscaSimulacaoSimplificadaResponseOK() throws Exception {

        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/simulacao/simplificada/60788051777b4827237ddc47")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("token", TOKEN);
        mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void shouldAtualizarDadosContatoResponseAccepted() throws Exception {
        DadosContatoRequest request = criarDadosContatoRequest("1234", CLIENTE_INATIVO_CPF, null, "ABRADIT", 12);
        RequestBuilder requestBuilder = MockMvcRequestBuilders.put("/simulacao/simplificada/dados-contato")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("token", TOKEN)
                .content(objectMapper.writeValueAsString(request));
        mockMvc.perform(requestBuilder).andExpect(status().isAccepted()).andReturn();
    }

    @Test
    public void shouldGerarTextoAvisoLegalResponseOK() throws Exception {
        TextoAvisoLegalRequest request = new TextoAvisoLegalRequest("60788051777b4827237ddc47", "1234", "ABRADIT", 12);
        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/simulacao/simplificada/aviso-legal")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("token", TOKEN)
                .content(objectMapper.writeValueAsString(request));
        mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
    }


}