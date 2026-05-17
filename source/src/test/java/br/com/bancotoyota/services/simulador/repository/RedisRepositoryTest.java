package br.com.bancotoyota.services.simulador.repository;

import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@JsonTest
public class RedisRepositoryTest {

    @MockBean
    private RedisTemplate<String, TaxasIOF> taxasRedisTemplate;
    
    @MockBean
    private RedisTemplate<String, Seguro> seguroRedisTemplate;

    @MockBean
    private ListOperations<String, TaxasIOF> taxasListOperations;

    @MockBean
    private SetOperations<String, TaxasIOF> seguroSetOperations;

    private RedisRepository repository;
    
    private RedisRepository repositoryMock = new RedisRepositoryMock();
    
    @MockBean
    private ListOperations<String, ControlesBA> controlesBAOperation;
    
    @MockBean
    private RedisTemplate<String, ControlesBA> controlesBARedisTemplate;
    
    @Before
    public void init() {
        when(taxasRedisTemplate.opsForList()).then(i -> taxasListOperations);
        when(seguroRedisTemplate.opsForSet()).then(i -> seguroSetOperations);
        when(controlesBARedisTemplate.opsForList()).then(i -> controlesBAOperation);
        repository = new RedisRepositoryImpl(taxasRedisTemplate, seguroRedisTemplate,controlesBARedisTemplate);
    }

    @Test(expected = EntityNotFoundException.class)
    public void findTaxasNotFound() {
        repository.findTaxasIOF("key");
    }

    @Test
    public void findTaxas() {
        TaxasIOF valor = new TaxasIOF("365","0.3","0.1");
        when(taxasListOperations.index(anyString(),anyLong())).then(i->valor);
        TaxasIOF taxasIOF = repository.findTaxasIOF("key");
        assertNotNull(taxasIOF);
        assertEquals(valor, taxasIOF);
    }
    
    @Test
    public void findTaxasMock() {
        TaxasIOF valor = new TaxasIOF("365.000000", "0.030000", "0.003800");
        TaxasIOF taxasIOF = repositoryMock.findTaxasIOF("key");
        assertNotNull(taxasIOF);
        assertEquals(valor.getValorbaseCalculo(), taxasIOF.getValorbaseCalculo());
        assertEquals(valor.getValorIOFAA(), taxasIOF.getValorIOFAA());
        assertEquals(valor.getValorIOFAD(), taxasIOF.getValorIOFAD());
    }
    
    @Test
    public void findControlesBA() {
        ControlesBA controlesBA = new ControlesBA();
        controlesBA.setDataAtualBA(LocalDateTime.now());
        when(controlesBAOperation.index(anyString(),anyLong())).then(i->controlesBA);
        ControlesBA retorno =  repository.getDataBA();
        assertNotNull(retorno);
        assertEquals(controlesBA, retorno);
    }
    
    @Test
    public void findControlesBAMock() {
        ControlesBA controlesBA = new ControlesBA();
        controlesBA.setDataAtualBA(LocalDateTime.now());
        when(controlesBAOperation.index(anyString(),anyLong())).then(i->controlesBA);
        ControlesBA retorno =  repositoryMock.getDataBA();
        assertNotNull(retorno);
    }
    
    
    @Test(expected = EntityNotFoundException.class)
    public void findControlesBAWhenResultIsNull() {
        ControlesBA controlesBA = null;
        when(controlesBAOperation.index(anyString(),anyLong())).then(i->controlesBA);
        repository.getDataBA();
    }
    
    
    @Test(expected = EntityNotFoundException.class)
    public void getSeguroNotFound() {
        repository.getSeguro("codigo");
    }

    @Test(expected = EntityNotFoundException.class)
    public void getSeguroNotFoundMock() {
        repositoryMock.getSeguro("codigo");
    }
    
    @Test
    public void getSeguro() {
        Seguro valor = new Seguro(TipoDeSeguroPrestamista.SPF, new BigDecimal(2500), new BigDecimal(100000),
                new BigDecimal(400000), new BigDecimal("0.02"), 18, 70);
        when(seguroSetOperations.members(anyString())).then(i->new HashSet<>(Arrays.asList(valor)));
        Seguro seguro = repository.getSeguro("codigo");
        assertNotNull(seguro);
        assertEquals(valor, seguro);
    }
    
    @Test
    public void getSeguroMock() {
        Seguro valorSPF = new Seguro(TipoDeSeguroPrestamista.SPF, new BigDecimal(2500), new BigDecimal("150000.00"),
                new BigDecimal("450000.00"), new BigDecimal("0.029000"), 18, 65);
        
        Seguro valorSVP = new Seguro(TipoDeSeguroPrestamista.SVP, BigDecimal.ZERO, new BigDecimal("330000.00"),
                new BigDecimal("990000.00"), new BigDecimal("0.012000"), 18, 65);

        Seguro seguroSPF = repositoryMock.getSeguro("SPF");
        Seguro seguroSVP = repositoryMock.getSeguro("SVP");
        assertNotNull(seguroSPF);
        assertNotNull(seguroSVP);
        
		assertEquals(valorSPF.getIdadeMaxima()			, seguroSPF.getIdadeMaxima());
        assertEquals(valorSPF.getIdadeMinima()			, seguroSPF.getIdadeMinima());
        assertEquals(valorSPF.getMaximoDaParcela()		, seguroSPF.getMaximoDaParcela());
        assertEquals(valorSPF.getMaximoDoContrato()		, seguroSPF.getMaximoDoContrato());
        assertEquals(valorSPF.getMaximoPorCliente()		, seguroSPF.getMaximoPorCliente());
        assertEquals(valorSPF.getPercentualDoSeguro()	, seguroSPF.getPercentualDoSeguro());
        assertEquals(valorSPF.getTipo()					, seguroSPF.getTipo());
		
		assertEquals(valorSVP.getIdadeMaxima()			, seguroSVP.getIdadeMaxima());
        assertEquals(valorSVP.getIdadeMinima()			, seguroSVP.getIdadeMinima());
        assertEquals(valorSVP.getMaximoDaParcela()		, seguroSVP.getMaximoDaParcela());
        assertEquals(valorSVP.getMaximoDoContrato()		, seguroSVP.getMaximoDoContrato());
        assertEquals(valorSVP.getMaximoPorCliente()		, seguroSVP.getMaximoPorCliente());
        assertEquals(valorSVP.getPercentualDoSeguro()	, seguroSVP.getPercentualDoSeguro());
        assertEquals(valorSVP.getTipo()					, seguroSVP.getTipo());
        
    }
    
}

