package br.com.bancotoyota.services.simulador;

import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcDesejadaRequest;
import br.com.bancotoyota.services.simulador.beans.response.*;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.controller.SimuladorParcDesejadaNovaController;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.CalculadoraSeguroMecanicaServices;
import br.com.bancotoyota.services.simulador.services.MotorTaxaPlanoService;
import br.com.bancotoyota.services.simulador.services.PlanoService;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import br.com.bancotoyota.services.simulador.utils.FabricaObjetosDeUmJsonUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.BooleanUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SimuladorParcDesejadaIT {
    private MockMvc mockMvc;
	@MockBean
	private MotorTaxaPlanoService motorTaxaPlanoService;
	@Mock
	private PlanoService planoService;
	@MockBean
	private RestTemplate restTemplate;
    @Autowired
    private ObjectMapper mapper;
	@MockBean
	private SeguroPrestamistaPlusServiceImpl prestamistaService;
	private FeatureToggleConfig featureToggleConfig = new FeatureToggleConfig(Boolean.TRUE, Boolean.FALSE);
    @Autowired
    private SimuladorParcDesejadaNovaController simuladorParcDesejadaController;

	@MockBean
	private CalculadoraSeguroMecanicaServices seguroGarantiaServices;
	private FabricaObjetosDeUmJsonUtil fabrica = new FabricaObjetosDeUmJsonUtil();


    @Before
    public void setUp() {


		mockMvc = MockMvcBuilders.standaloneSetup(simuladorParcDesejadaController).build();
    }

    @Test
	@Ignore
	public void deveRealizarSimulacaoParcelaPassandoCNPJComCPFSemLimiteEstouradoComSeguroExcluido() throws JsonMappingException, JsonProcessingException, UnsupportedEncodingException, IOException, Exception {
		executarTeste("prestamista-plus/request-parcela-com-cliente-por-prazo-com-cnpj -sem-cliente-estourado-com-seguro-excluido.json");
	}
	
	private void executarTeste(String caminhoArquivo)
			throws IOException, JsonProcessingException, JsonMappingException, Exception, UnsupportedEncodingException {
		SimulacaoParcDesejadaRequest request = fabrica.criarObjetoDoJson(
				caminhoArquivo, SimulacaoParcDesejadaRequest.class);

		String json = mapper.writeValueAsString(request);
		MvcResult resultado = mockMvc.perform(post("/parcelas/desejada/simulacao").contentType(MediaType.APPLICATION_JSON)
				.param("plano-id","2806")
				.param("percentual-entrada","25")
				.content(json).accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().is2xxSuccessful()).andReturn();
		
		SimulacaoParcDesejadaResponse resultadoRetornado = mapper.readValue(resultado.getResponse().getContentAsString(),SimulacaoParcDesejadaResponse.class);
		
		for(PrazoDesejadaResponse prazo : resultadoRetornado.getPrazos()) {
			if(!prazo.getTipoDeSeguroPrestamista().equals(TipoDeSeguroPrestamista.NENHUM)) {
				assertNotNull(prazo.getDadosSeguroPrestamistaUtilizado());
				assertTrue(prazo.getDadosSeguroPrestamistaUtilizado().getTipo().equals(prazo.getTipoDeSeguroPrestamista()));
			}
		}
	}
	private SeguroPrestamistaPlusResponse mockSeguroPrestamistPlus(){
		SeguroPrestamistaPlusResponse seguroPrestamista = new SeguroPrestamistaPlusResponse();
		Seguro seguro = new Seguro();
		seguro.setTipo(TipoDeSeguroPrestamista.SVP);
		seguro.setMaximoDaParcela(new BigDecimal("0.0"));
		seguro.setMaximoDoContrato(new BigDecimal("330000.0"));
		seguro.setMaximoPorCliente(new BigDecimal("990000.0"));
		seguro.setPercentualDoSeguro(new BigDecimal("0.016"));
		seguro.setIdadeMinima(18);
		seguro.setIdadeMaxima(65);
		seguro.setDescricao("SEGURO VIDA PRESTAMISTA");
		seguro.setFatorDesemprego(0.0);
		seguro.setFatorInvalidez(5.0);
		seguro.setFatorMorte(0.0155);
		seguro.setFormaCalculoDescricao("Sobre Valor Financiado");
		seguro.setFormaCalculoValor("VLR_FINANCIADO");
		seguro.setInicioVigencia(LocalDate.of(2022,07,01));
		seguro.setItemId(3);
		seguro.setOrdenacao(2);
		seguro.setPercentualIofSpf(0.38);
		seguro.setPercentualProLabore(30.0);
		seguro.setPrazoFinal(60);
		seguro.setPrazoInicio(1);
		seguro.setTempoMaximoContrato(60);

		seguroPrestamista.setUltimoSeguro(seguro);
		seguroPrestamista.setSeguros(new ArrayList<>(Arrays.asList(seguro)));

		return seguroPrestamista;
	}
	private void carregaPrazosByToggle(List<Prazo> lista) {
		Plano plano = new Plano();
		plano.setCodigoModalidade("ciclo-toyota");

		when(planoService.getPlanoById(any())).thenReturn(plano);
		if(BooleanUtils.isTrue(featureToggleConfig.getBuscaApiMotorTaxaEnabled())) {
			when(motorTaxaPlanoService.findPrazos(any(), any())).then(i -> lista);
			when(motorTaxaPlanoService.getPlanoPorId(any())).thenReturn(new PlanoMotorTaxa());
			when(motorTaxaPlanoService.convertPlanoFromMotorTaxa(motorTaxaPlanoService.getPlanoPorId(anyInt()))).thenReturn(plano);

		} else {
			when(planoService.findPrazos(any(), any())).then(i -> lista);

		}
	}

}
