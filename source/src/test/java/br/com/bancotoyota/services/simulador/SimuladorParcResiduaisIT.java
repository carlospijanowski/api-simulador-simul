package br.com.bancotoyota.services.simulador;

import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.response.PlanoMotorTaxa;
import br.com.bancotoyota.services.simulador.beans.response.PrazoResidualResponse;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcResidualResponse;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.controller.SimuladorParcResidualController;

import br.com.bancotoyota.services.simulador.entities.Plano;
import br.com.bancotoyota.services.simulador.entities.Prazo;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.entities.SeguroPrestamistaPlusResponse;
import br.com.bancotoyota.services.simulador.services.MotorTaxaPlanoService;
import br.com.bancotoyota.services.simulador.services.PlanoService;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import br.com.bancotoyota.services.simulador.utils.FabricaObjetosDeUmJsonUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.BooleanUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SimuladorParcResiduaisIT {
	private FeatureToggleConfig featureToggleConfig = new FeatureToggleConfig(Boolean.TRUE, Boolean.FALSE);

	private MockMvc mockMvc;
	@MockBean
	private MotorTaxaPlanoService motorTaxaPlanoService;
	@MockBean
	private RestTemplate restTemplate;
	@Mock
	private PlanoService planoService;
	@Autowired
	private ObjectMapper mapper;
	@MockBean
	private SeguroPrestamistaPlusServiceImpl prestamistaService;
	@Autowired
	private SimuladorParcResidualController simuladorParcResidualController;

	private FabricaObjetosDeUmJsonUtil fabrica = new FabricaObjetosDeUmJsonUtil();

	@Before
	public void setUp() {
		List<Prazo> lista = new ArrayList<>();

		lista.add(new Prazo(12, BigDecimal.ONE, new BigDecimal(20), new BigDecimal(50), new BigDecimal(0), new BigDecimal(0),null));
		lista.add(new Prazo(36, BigDecimal.ONE, new BigDecimal(20), new BigDecimal(50), new BigDecimal(0), new BigDecimal(0),null));
		lista.add(new Prazo(18, BigDecimal.ONE, new BigDecimal(20), new BigDecimal(50), new BigDecimal(20), new BigDecimal(50),null));
		lista.add(new Prazo(18, BigDecimal.ONE, new BigDecimal(20), new BigDecimal(50), new BigDecimal(51), new BigDecimal(70),null));

		carregaPrazosByToggle(lista);
		when(prestamistaService.getSegurosPrestamista(any(),any(), any(), anyBoolean(), anyBoolean())).
				thenReturn(mockSeguroPrestamistPlus()).thenReturn(mockSeguroPrestamistPlus());
		mockMvc = MockMvcBuilders.standaloneSetup(simuladorParcResidualController).build();
	}

	@Test
	public void deveRealizarSimulacaoResidualSemClientePorPrazo() throws Exception {
		executarTeste("prestamista-plus/request-residual-sem-cliente-por-prazo.json");
	}

	@Test
	public void deveRealizarSimulacaoResidualPassandoCNPJ() throws JsonMappingException, JsonProcessingException, UnsupportedEncodingException, IOException, Exception {
		executarTeste("prestamista-plus/request-residual-sem-cliente-por-prazo-com-cnpj.json");
	}
	
	@Test
	public void deveRealizarSimulacaoResidualPassandoCNPJComCPFComLimiteEstourado() throws JsonMappingException, JsonProcessingException, UnsupportedEncodingException, IOException, Exception {
		executarTeste("prestamista-plus/request-residual-sem-cliente-por-prazo-com-cnpj -com-cliente-estourado.json");
	}
	
	@Test
	public void deveRealizarSimulacaoResidualPassandoCNPJComCPFSemLimiteEstouradoComSeguroExcluido() throws JsonMappingException, JsonProcessingException, UnsupportedEncodingException, IOException, Exception {
		executarTeste("prestamista-plus/request-residual-com-cliente-por-prazo-com-cnpj -sem-cliente-estourado-com-seguro-excluido.json");
	}
	
	@Test
	public void deveRealizarSimulacaoResidualPassandoCNPJComCPFSemLimiteEstouradoComSeguroEscolhido() throws JsonMappingException, JsonProcessingException, UnsupportedEncodingException, IOException, Exception {
		executarTeste("prestamista-plus/request-residual-com-cliente-por-prazo-com-cnpj -com-cliente-estourado-com-seguro-escolhido.json");
	}
	
	private void executarTeste(String caminhoArquivo)
			throws IOException, JsonProcessingException, JsonMappingException, Exception, UnsupportedEncodingException {
		SimulacaoParcResidualRequest request = fabrica.criarObjetoDoJson(
				caminhoArquivo, SimulacaoParcResidualRequest.class);

		String json = mapper.writeValueAsString(request);
		MvcResult resultado = mockMvc.perform(post("/parcelas/residual/simulacao").contentType(MediaType.APPLICATION_JSON)
				.content(json).accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().is2xxSuccessful()).andReturn();
		
		SimulacaoParcResidualResponse resultadoRetornado = mapper.readValue(resultado.getResponse().getContentAsString(),SimulacaoParcResidualResponse.class);
		
		for(PrazoResidualResponse prazo : resultadoRetornado.getPrazos()) {
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
