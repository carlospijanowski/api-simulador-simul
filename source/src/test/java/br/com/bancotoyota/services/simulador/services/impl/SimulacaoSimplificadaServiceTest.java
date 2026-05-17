package br.com.bancotoyota.services.simulador.services.impl;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.UpdateResult;

import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoSimplificadaDesejadaRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoSimplificadaResidualRequest;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoSimplificadaDesejadaResponse;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoSimplificadaResidualResponse;
import br.com.bancotoyota.services.simulador.controller.SimuladorSimplificadoControllerBaseTest;
import br.com.bancotoyota.services.simulador.entities.ClienteProspect;
import br.com.bancotoyota.services.simulador.entities.OrigemSolicitacao;
import br.com.bancotoyota.services.simulador.entities.SimulacaoSimplificada;
import br.com.bancotoyota.services.simulador.repository.simulacoes.SimulacaoSimplificadaRepository;
import br.com.bancotoyota.services.simulador.services.OrigemSolicitacaoService;
import br.com.bancotoyota.services.simulador.services.SimulacaoSimplificadaService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "/application.properties")
public class SimulacaoSimplificadaServiceTest extends SimuladorSimplificadoControllerBaseTest{
	@MockBean
	private SimulacaoSimplificadaRepository repository;
	
	@MockBean
	private MongoTemplate mongoTemplate;	
	
	@MockBean
	private OrigemSolicitacaoService origemService;

	private SimulacaoSimplificadaService simulacaoSimplificadaService;
	
	private SimulacaoSimplificada simulacaoSimplificada1;
	
	private SimulacaoSimplificadaDesejadaRequest requestDesejada;
	private SimulacaoSimplificada simulacaoSimplificadaDesejada;
	private SimulacaoRequest simulacaoRequestDesejada;
	private SimulacaoSimplificadaDesejadaResponse simulacaoSimplificadaDesejadaResponse;
	
	private SimulacaoSimplificadaResidualRequest requestResidual;
	private SimulacaoSimplificada simulacaoSimplificadaResidual;
	private SimulacaoParcResidualRequest simulacaoRequestResidual;
	private SimulacaoSimplificadaResidualResponse simulacaoSimplificadaResidualResponse;
	
	private ClienteProspect clienteProspect;
	
    @Before
    public void setup() {
    	
    	simulacaoSimplificadaService = new SimulacaoSimplificadaServiceImpl(repository,mongoTemplate,origemService);
    	
        try {
        	
        	clienteProspect = new ClienteProspect();
        	clienteProspect.setCelular("11999999999");
        	clienteProspect.setCodigoContrato("codigoContrato");
        	clienteProspect.setCpfCnpj("99999999999");
        	clienteProspect.setEmail("teste@simulador.com");
        	clienteProspect.setIdOfertaSalesforce("idSalesforce");
        	clienteProspect.setNome("Nome Teste");
        	clienteProspect.setPrazoSelecionado(12);
        	clienteProspect.setTelefone("1199999999");
        	
        	clienteProspect.getCelular();
        	clienteProspect.getCodigoContrato();
        	clienteProspect.getCpfCnpj();
        	clienteProspect.getEmail();
        	clienteProspect.getIdOfertaSalesforce();
        	clienteProspect.getNome();
        	clienteProspect.getPrazoSelecionado();
        	clienteProspect.getTelefone();           	
        	
        	ObjectMapper mapper = new ObjectMapper();
			simulacaoSimplificada1 = mapper.readValue(
			        lerArquivo(getClass().getClassLoader().getResourceAsStream("dados-simulacao-simplificada.json")), SimulacaoSimplificada.class);
			
			BigDecimal valorParcelaDesejada = BigDecimal.valueOf(1000);
			requestDesejada = criarSimulacaoSimplificadaDesejadaRequest(valorParcelaDesejada);
			simulacaoSimplificadaDesejada = mapper.readValue(
			        lerArquivo(getClass().getClassLoader().getResourceAsStream("./simplificada/dados-simulacao-simplificada-desejada.json")), SimulacaoSimplificada.class);
			simulacaoRequestDesejada = mapper.readValue(
			        lerArquivo(getClass().getClassLoader().getResourceAsStream("./simplificada/dados-simulacao-request-desejada.json")), SimulacaoRequest.class);
			simulacaoSimplificadaDesejadaResponse = mapper.readValue(
			        lerArquivo(getClass().getClassLoader().getResourceAsStream("./simplificada/dados-simulacao-simplificada-desejada-response.json")), SimulacaoSimplificadaDesejadaResponse.class);
			
			BigDecimal valorParcelaResidual = BigDecimal.valueOf(10000);
			requestResidual = criarSimulacaoSimplificadaResidualRequest(new Integer[] { 12, 24, 36 }, valorParcelaResidual);
			simulacaoSimplificadaResidual = mapper.readValue(
			        lerArquivo(getClass().getClassLoader().getResourceAsStream("./simplificada/dados-simulacao-simplificada-residual.json")), SimulacaoSimplificada.class);
			simulacaoRequestResidual = mapper.readValue(
			        lerArquivo(getClass().getClassLoader().getResourceAsStream("./simplificada/dados-simulacao-request-residual.json")), SimulacaoParcResidualRequest.class);
			simulacaoSimplificadaResidualResponse = mapper.readValue(
			        lerArquivo(getClass().getClassLoader().getResourceAsStream("./simplificada/dados-simulacao-simplificada-residual-response.json")), SimulacaoSimplificadaResidualResponse.class);
			
			//INSERT
			OrigemSolicitacao origemSolicitacao = new OrigemSolicitacao();
			origemSolicitacao.setDataGravacao(LocalDate.now());
			origemSolicitacao.setDescricao("SITE BTB");
			origemSolicitacao.setId("1fe4f5ea4895fea");
			origemSolicitacao.setOrigemSolicitacao("INSTITUCIONAL");
			
			//when(mongoTemplate.findOne(any(), eq(OrigemSolicitacao.class), anyString())).thenReturn(origemSolicitacao);
			when(repository.insert(simulacaoSimplificada1)).thenReturn(simulacaoSimplificada1);
			when(origemService.findOrigemSolicitacao(anyString())).thenReturn(origemSolicitacao);
			
			//BUSCA
			when(mongoTemplate.findOne(any(), eq(SimulacaoSimplificada.class), any())).thenReturn(simulacaoSimplificada1);
			
			List<Object> lista = new ArrayList<Object>();
			lista.add(simulacaoSimplificada1);
			//ATUALIZACAO
			UpdateResult result = UpdateResult.acknowledged(new Long(1), new Long(1), null);
			when(mongoTemplate.updateMulti(any(), any(), eq(SimulacaoSimplificada.class))).thenReturn(result);
			when(mongoTemplate.find(any(), any(), any())).thenReturn(lista);
			when(repository.save(any())).thenReturn(simulacaoSimplificada1);
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}    	
    	
    }
    
    @Test
    public void deveRetornarSimulacaoSimplificadaDesejada() {
    	
    	
    	SimulacaoSimplificada sim = simulacaoSimplificadaService.simplificadaDesejadaToSimulacaoSimplificada(simulacaoSimplificadaDesejada, simulacaoRequestDesejada, simulacaoSimplificadaDesejadaResponse, requestDesejada);
    	
    	//validação dos prazos
    	Assert.assertEquals(sim.getSaida().getSimulacao().getPrazos().get(0).getValorParcela(), simulacaoSimplificadaDesejadaResponse.getPrazos().get(0).getValorParcela());
    	//valorEntrada
    	Assert.assertEquals(sim.getSaida().getSimulacao().getValorEntrada(), simulacaoRequestDesejada.getValorEntrada());
    	//valorUfEmplacamento
    	Assert.assertEquals(sim.getSaida().getSimulacao().getValorUfEmplacamento(), simulacaoRequestDesejada.getValorUfEmplacamento());
    }
    
    
    @Test
    public void deveRetornarSimulacaoSimplificadaResidual() {
    	SimulacaoSimplificada sim = simulacaoSimplificadaService.simplificadaResidualToSimulacaoSimplificada(simulacaoSimplificadaResidual, simulacaoRequestResidual, simulacaoSimplificadaResidualResponse, requestResidual);
    	
    	//validação dos prazos
    	Assert.assertEquals(sim.getSaida().getSimulacao().getPrazos().get(0).getValorParcela(), simulacaoSimplificadaResidualResponse.getPrazos().get(0).getValorParcela());
    	//valorEntrada
    	Assert.assertEquals(sim.getSaida().getSimulacao().getValorEntrada(), simulacaoRequestResidual.getValorEntrada());
    	//valorUfEmplacamento
    	Assert.assertEquals(sim.getSaida().getSimulacao().getValorUfEmplacamento(), simulacaoRequestResidual.getValorUfEmplacamento());
    }
    
    @Test
    public void deveEncontrarSimulacaoSimplificada() {
    	SimulacaoSimplificada retorno = simulacaoSimplificadaService.findByCodigoSimulacao(simulacaoSimplificada1.getId());
    	Assert.assertEquals(simulacaoSimplificada1.getId(), retorno.getId());
    }
    
    @Test
    public void deveEncontrarSimulacaoSimplificadaSessionId() {
    	SimulacaoSimplificada retorno = simulacaoSimplificadaService.findByCodigoSimulacaoSessionIdOrigem(simulacaoSimplificada1.getId(), simulacaoSimplificada1.getSessionId(), simulacaoSimplificada1.getOrigemSolicitacao());
    	Assert.assertEquals(simulacaoSimplificada1.getId(), retorno.getId());
    }    
    
    @Test(expected = EntityNotFoundException.class)
    public void deveRetornarErroSessionIdNaoEncontrado() {
    	simulacaoSimplificadaService.findByCodigoSimulacaoSessionIdOrigem(simulacaoSimplificada1.getId(), "333133-35464-343uifadbe4x", simulacaoSimplificada1.getOrigemSolicitacao());
    }     
    
    @Test
    public void deveEncontrarSimulacaoSimplificadaPrazo() {
    	SimulacaoSimplificada retorno = simulacaoSimplificadaService.findByCodigoSimulacaoSessionIdOrigemPrazo(simulacaoSimplificada1.getId(), simulacaoSimplificada1.getSessionId(), simulacaoSimplificada1.getOrigemSolicitacao(), 12);
    	Assert.assertEquals(simulacaoSimplificada1.getId(), retorno.getId());
    }       
    
    @Test(expected = EntityNotFoundException.class)
    public void deveRetornarErroPrazoNaoEncontrado() {
    	simulacaoSimplificadaService.findByCodigoSimulacaoSessionIdOrigemPrazo(simulacaoSimplificada1.getId(), simulacaoSimplificada1.getSessionId(), simulacaoSimplificada1.getOrigemSolicitacao(), 6);
    }       
    
    @Test(expected = EntityNotFoundException.class)
    public void deveRetornarErroNehumaSimulacaoEncontrada() {
		when(mongoTemplate.findOne(any(), any(), any())).thenReturn(null);
		simulacaoSimplificadaService.findByCodigoSimulacao("1234");
    }      
    
    @Test
    public void deveRetornarCodigoSimulacao() {
    	String codigoSimulacao = simulacaoSimplificadaService.insertSimulacaoSimplificada(simulacaoSimplificada1);
    	Assert.assertEquals(simulacaoSimplificada1.getId(), codigoSimulacao);
    }    
 
    
    @Test
    public void deveAtualizarDadosContato() {
   	
    	simulacaoSimplificadaService.updateDadosContatoBySessionId(simulacaoSimplificada1.getSessionId(), simulacaoSimplificada1.getOrigemSolicitacao(), clienteProspect);
    	String codigoSimulacao = simulacaoSimplificadaService.insertSimulacaoSimplificada(simulacaoSimplificada1);
    	Assert.assertEquals(simulacaoSimplificada1.getId(), codigoSimulacao);
    }  
    
    @Test(expected = EntityNotFoundException.class)
    public void atualizarDadosContatoComErro() {
   	
    	UpdateResult result1 = UpdateResult.acknowledged(new Long(0), new Long(0), null);
		when(mongoTemplate.updateMulti(any(), any(), eq(SimulacaoSimplificada.class))).thenReturn(result1);
		
    	simulacaoSimplificadaService.updateDadosContatoBySessionId(simulacaoSimplificada1.getSessionId(), simulacaoSimplificada1.getOrigemSolicitacao(), clienteProspect);
    	
    }     
    
    
    @Test
    public void deveAtualizarDadosSimulacao() {
   	
    	simulacaoSimplificadaService.updateSimulacaoSelecionadaByCodigoSimulacao(simulacaoSimplificada1.getOrigemSolicitacao(), clienteProspect);
    	String codigoSimulacao = simulacaoSimplificadaService.insertSimulacaoSimplificada(simulacaoSimplificada1);
    	Assert.assertEquals(simulacaoSimplificada1.getId(), codigoSimulacao);
    }      
    
    
    @Test(expected = EntityNotFoundException.class)
    public void atualizarDadosSimulacaoComErro() {
    	
    	UpdateResult result1 = UpdateResult.acknowledged(new Long(0), new Long(0), null);
		when(mongoTemplate.updateMulti(any(), any(), eq(SimulacaoSimplificada.class))).thenReturn(result1);
   	
    	simulacaoSimplificadaService.updateSimulacaoSelecionadaByCodigoSimulacao(simulacaoSimplificada1.getOrigemSolicitacao(), clienteProspect);
    }        
	
    protected String lerArquivo(InputStream resource) throws IOException {
        return IOUtils.toString(resource, "UTF-8");
    }     
    
    @Test
    public void deveRetornarOrigemSemDescricao() {
    	
    	OrigemSolicitacao origemSolicitacao = new OrigemSolicitacao();
		origemSolicitacao.setDataGravacao(LocalDate.now());
		origemSolicitacao.setDescricao(null);
		origemSolicitacao.setId("1fe4f5ea4895fea");
		origemSolicitacao.setOrigemSolicitacao("INSTITUCIONAL");    	
    	
    	when(origemService.findOrigemSolicitacao(any())).thenReturn(origemSolicitacao);
    	String codigoSimulacao = simulacaoSimplificadaService.insertSimulacaoSimplificada(simulacaoSimplificada1);
    	Assert.assertEquals(codigoSimulacao, simulacaoSimplificada1.getId());
    	
    	
    	origemSolicitacao.setDescricao("");
    	when(origemService.findOrigemSolicitacao(any())).thenReturn(origemSolicitacao);
    	codigoSimulacao = simulacaoSimplificadaService.insertSimulacaoSimplificada(simulacaoSimplificada1);
    	Assert.assertEquals(codigoSimulacao, simulacaoSimplificada1.getId());    	
    	
    }    
    
    
    
    @Test
    public void deveRetornarCodigoSimulacaoNull() {
    	
    	    	
    	
    	simulacaoSimplificada1.setOrigemSolicitacao(null);
    	String codigoSimulacao = simulacaoSimplificadaService.insertSimulacaoSimplificada(simulacaoSimplificada1);
    	Assert.assertEquals(codigoSimulacao, null);
    	
    	simulacaoSimplificada1.setOrigemSolicitacao("");
    	codigoSimulacao = simulacaoSimplificadaService.insertSimulacaoSimplificada(simulacaoSimplificada1);
    	Assert.assertEquals(codigoSimulacao, null);   
    	
    	simulacaoSimplificada1.setOrigemSolicitacao("ORIGEM_INEXISTENTE");
    	when(origemService.findOrigemSolicitacao(any())).thenThrow(EntityNotFoundException.class);
    	codigoSimulacao = simulacaoSimplificadaService.insertSimulacaoSimplificada(simulacaoSimplificada1);
    	Assert.assertEquals(codigoSimulacao, null);  
    	
    	
    }        
    
}
