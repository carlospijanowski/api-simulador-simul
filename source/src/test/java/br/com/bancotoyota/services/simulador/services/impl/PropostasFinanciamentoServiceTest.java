package br.com.bancotoyota.services.simulador.services.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.bancotoyota.services.simulador.entities.PropostaAvisoLegal;
import br.com.bancotoyota.services.simulador.entities.PropostaTextoAvisoLegalResponse;
import br.com.bancotoyota.services.simulador.entities.SimulacaoAvisoLegal;
import br.com.bancotoyota.services.simulador.entities.SimulacaoSimplificada;
import br.com.bancotoyota.services.simulador.services.PropostasFinanciamentoService;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "/application.properties")
public class PropostasFinanciamentoServiceTest {
	
	@MockBean
    private RestTemplate restTemplate;
	
	@Value("${api.propostas-financiamento.url:}")
    private String apiPropostasUrl;
	
	private PropostasFinanciamentoService propostasFinanciamentoService;
	
	private SimulacaoSimplificada simulacaoSimplificada;
	
	private static final String TEXTO_AVISO_LEGAL = "TextoAvisoLegal";
	
    @Before
    public void setup() {
    	
    	ObjectMapper mapper = new ObjectMapper();
		try {
			simulacaoSimplificada = mapper.readValue(
			        lerArquivo(getClass().getClassLoader().getResourceAsStream("dados-simulacao-simplificada.json")), SimulacaoSimplificada.class);
   	
    	
	    	propostasFinanciamentoService = new PropostasFinanciamentoServiceImpl(restTemplate, apiPropostasUrl);
	    	
	    	//GERAR TEXTO AVISO LEGAL
	    	PropostaTextoAvisoLegalResponse textoAvisoLegal = new PropostaTextoAvisoLegalResponse(TEXTO_AVISO_LEGAL);
	    	when(restTemplate.exchange(eq(apiPropostasUrl.concat("/aviso-legal")), any(), any(), eq(PropostaTextoAvisoLegalResponse.class)))
	        .thenReturn(new ResponseEntity<>(textoAvisoLegal, HttpStatus.OK));
	    	
	    	
    	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}     	
    }
    
    @Test(expected = InstantiationError.class)
    public void deveRetornarErroNehumaSimulacaoEncontrada() {
    	when(restTemplate.exchange(eq(apiPropostasUrl.concat("/aviso-legal")), any(), any(), eq(PropostaTextoAvisoLegalResponse.class)))
        .thenThrow(InstantiationError.class);
    	PropostaAvisoLegal proposta = new PropostaAvisoLegal();
    	propostasFinanciamentoService.gerarTextoAvisoLegal(proposta);
    }
    
    //GERAR TEXTO AVISO LEGAL SIMULACAO SIMPLIFICADA
    @Test
    public void deveGerarTextoAvisoLegal() {
    	String avisoLegal = propostasFinanciamentoService.gerarTextoAvisoLegalSimulacaoSimplificada(simulacaoSimplificada, 12);
    	
    	//validação do texto
    	Assert.assertEquals(TEXTO_AVISO_LEGAL, avisoLegal);
    	
    }
    
    @Test(expected = BusinessValidationException.class)
    public void deveRetornarErroDataPrimeiroVencimento() {
    	SimulacaoSimplificada sim = simulacaoSimplificada;
    	sim.getEntrada().getDetalheSimulacao().setDataPrimeiroVencimento(LocalDate.now());
    	propostasFinanciamentoService.gerarTextoAvisoLegalSimulacaoSimplificada(sim, 5);
    }    
    
    @Test(expected = EntityNotFoundException.class)
    public void deveRetornarErroPrazoNaoEncontrado() {
    	propostasFinanciamentoService.gerarTextoAvisoLegalSimulacaoSimplificada(simulacaoSimplificada, 6);
    }      
	
    protected String lerArquivo(InputStream resource) throws IOException {
        return IOUtils.toString(resource, "UTF-8");
    }  

}

