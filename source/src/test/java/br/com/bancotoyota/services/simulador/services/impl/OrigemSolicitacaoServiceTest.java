package br.com.bancotoyota.services.simulador.services.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import br.com.bancotoyota.services.simulador.entities.OrigemSolicitacao;
import br.com.bancotoyota.services.simulador.services.OrigemSolicitacaoService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "/application.properties")
public class OrigemSolicitacaoServiceTest {
	
	@MockBean
	private MongoTemplate mongoTemplate;
	
	private OrigemSolicitacaoService origemSolicitacaoService;
	
	OrigemSolicitacao origemSolicitacao;
	
	@Before
    public void setup() {
		
		origemSolicitacaoService = new OrigemSolicitacaoServiceImpl(mongoTemplate);
		
		origemSolicitacao = new OrigemSolicitacao();
		origemSolicitacao.setId("123far1as123dassdf");
		origemSolicitacao.setOrigemSolicitacao("INSTITUCIONAL");
		origemSolicitacao.setDescricao("SITE BTB");
		origemSolicitacao.setDataGravacao(LocalDate.now());
		
		origemSolicitacao.getId();
		origemSolicitacao.getOrigemSolicitacao();
		origemSolicitacao.getDescricao();
		origemSolicitacao.getDataGravacao();
		
		when(mongoTemplate.findOne(any(), eq(OrigemSolicitacao.class), any())).thenReturn(origemSolicitacao);
		
	}

	@Test
    public void deveRetornarOrigemSolicitacao() {
		OrigemSolicitacao origem = origemSolicitacaoService.findOrigemSolicitacao("INSTITUCIONAL");
		Assert.assertEquals(origem, origemSolicitacao);
	}
	
	@Test(expected = EntityNotFoundException.class)
    public void deveRetornarErroOrigemSolicitacao() {
		when(mongoTemplate.findOne(any(), eq(OrigemSolicitacao.class), any())).thenReturn(null);
		OrigemSolicitacao origem = origemSolicitacaoService.findOrigemSolicitacao("INSTITUCIONAL");
		Assert.assertEquals(origem, origemSolicitacao);
	}	
	
}
