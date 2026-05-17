package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.request.RestaUmRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoSimplificadaDesejadaRequest;
import br.com.bancotoyota.services.simulador.beans.response.AlternativaPrazoResponse;
import br.com.bancotoyota.services.simulador.beans.response.AlternativaResponse;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.RestaUmService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(RestaUmController.class)
public class RestaUmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RestaUmService restaUmService;

    @Test
    public void quandoForCDCParcelaDesejadaEntaoRetornaAlternativaCiclo() throws Exception {
        when(this.restaUmService.opcaoCicloToyota(any(), anyString(), anyString()))
                .thenReturn(new AlternativaResponse());

        RestaUmRequest request = buildRequestRestaUm(false);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/desejada/alternativa")
                .header("perfil", "REPRESENTANTE")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    public void quandoForCDCAPrazoEntaoRetornaAlternativaCiclo() throws Exception {
        when(this.restaUmService.opcaoCicloToyotaAPrazo(any(), anyString(), anyString()))
                .thenReturn(new AlternativaPrazoResponse());

        RestaUmRequest request = buildRequestRestaUm(false);
        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/residual/alternativa")
                .header("perfil", "REPRESENTANTE")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    public void quandoRequisicaoSemPerfilEntaoRetorna400() throws Exception {
        when(this.restaUmService.opcaoCicloToyotaAPrazo(any(), anyString(), anyString()))
                .thenReturn(new AlternativaPrazoResponse());

        RestaUmRequest request = buildRequestRestaUm(false);
        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/residual/alternativa")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    private RestaUmRequest buildRequestRestaUm(boolean isDesejada) {
        RestaUmRequest request = new RestaUmRequest();
        ConfiguracaoPlano configuracaoPlano = new ConfiguracaoPlano(Modalidade.CICLO_TOYOTA, TipoPessoa.PESSOA_FISICA, SituacaoVeiculo.ZERO_KM, false, false, 4176);
        SimulacaoSimplificadaDesejadaRequest parametros = new SimulacaoSimplificadaDesejadaRequest();
        parametros.setValorBem(new BigDecimal("100000"));
        parametros.setValorEntrada(new BigDecimal("60000"));
        parametros.setUfEmplacamento(UfEmplacamento.ES);
        parametros.setCnpjOrigemNegocio("91919940439446");
        parametros.setMarcaId("002");
        parametros.setModeloId("002182-2&03");
        parametros.setAnoModelo(2023);
        parametros.setSeguroAuto(BigDecimal.ZERO);
        parametros.setItens(new ArrayList<>());
        parametros.setSvpRemovido(false);
        parametros.setSpfRemovido(false);
        if (isDesejada) {
            parametros.setValorParcelaDesejada(new BigDecimal("3000"));
        } else {
            parametros.setParcelas(new Integer[]{22});
        }

        request.setParametros(parametros);
        request.setConfiguracao(configuracaoPlano);
        return request;
    }
}