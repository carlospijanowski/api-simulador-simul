package br.com.bancotoyota.services.simulador.services.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;



import br.com.bancotoyota.services.simulador.entities.MarcaVeiculos;

import br.com.bancotoyota.services.simulador.entities.ModeloVeiculos;



import br.com.bancotoyota.services.simulador.entities.VeiculosResponse;
import br.com.bancotoyota.services.simulador.services.ApiService;
import br.com.bancotoyota.services.simulador.services.VeiculosService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "/application.properties")
public class VeiculosServiceTest {
	
	@MockBean
    private RestTemplate restTemplate;
	@MockBean
    private ApiService apiService;
	
	@Value("${api.veiculos.url:}")
    private String apiVeiculosUrl;
	
	private static final String MARCA_ID_TOYOTA = "002";
	private static final String MODELO_ID_COROLLA = "002112";
	private static final String MODELO_ID_CAMRY = "002010";
	
	private VeiculosService veiculosService;
	
	private ModeloVeiculos modelo1;
	private ModeloVeiculos modelo2;
	private ModeloVeiculos modelo3;
	private ModeloVeiculos modelo4;
	private ModeloVeiculos modelo5;	
	private ModeloVeiculos modelo6;	
	private List<ModeloVeiculos> modelos;
	private List<ModeloVeiculos> modelos1;
	private List<ModeloVeiculos> modelos2;
	
    @Before
    public void setup() {
    	veiculosService = new VeiculosServiceImpl(restTemplate, apiService, apiVeiculosUrl);
    	
    	MarcaVeiculos marca = new MarcaVeiculos("TOYOTA", "002");
    	modelo1 = new ModeloVeiculos("2002", "2002", "V", "002010", null, "CAMRY XLE 3.0 24V 186CV", null, null, marca, null);
    	modelo2 = new ModeloVeiculos("2020", "2020", "V", "002112", null, "COROLLA ALTIS 2.0 FLEX 16V AUT.", null, null, marca, null);
    	
    	modelo3 = new ModeloVeiculos("9999", "9999", "V", "002010", null, "CAMRY XLE 3.0 24V 186CV", null, null, marca, null);
    	modelo4 = new ModeloVeiculos("9999", "9999", "V", "002112", null, "COROLLA ALTIS 2.0 FLEX 16V AUT.", null, null, marca, null);
    	
    	modelos = new ArrayList<>();
    	modelos.add(modelo1);
    	modelos.add(modelo2);
    	modelos.add(modelo3);
    	modelos.add(modelo4);
    	
    	VeiculosResponse veiculosResponse = new VeiculosResponse(new VeiculosResponse.ResponseVeiculos(modelos));
    	
    	when(restTemplate.exchange(eq(apiVeiculosUrl.concat("/marcas/002/modelos")), any(), any(), eq(VeiculosResponse.class)))
        .thenReturn(new ResponseEntity<>(veiculosResponse, HttpStatus.OK));
    	
    	when(restTemplate.exchange(eq(apiVeiculosUrl.concat("/marcas/null/modelos?ano-modelo=2020")), any(), any(), eq(VeiculosResponse.class)))
        .thenReturn(new ResponseEntity<>(veiculosResponse, HttpStatus.BAD_REQUEST));

    	Integer anoModelo = LocalDate.now().get(ChronoField.YEAR);
    	
    	MarcaVeiculos marca1 = new MarcaVeiculos("TOYOTA", "001");
    	modelo5 = new ModeloVeiculos(anoModelo.toString(), anoModelo.toString(), "V", "002010", null, "CAMRY XLE 3.0 24V 186CV", null, null, marca1, null);
    	modelo6 = new ModeloVeiculos(anoModelo.toString(), anoModelo.toString(), "V", "002112", null, "COROLLA ALTIS 2.0 FLEX 16V AUT.", null, null, marca1, null);
    	
    	modelos2 = new ArrayList<>();
    	modelos2.add(modelo5);
    	modelos2.add(modelo6);     	
    	
    	VeiculosResponse veiculosResponse3 = new VeiculosResponse(new VeiculosResponse.ResponseVeiculos(modelos2));
    	
    	
    	when(restTemplate.exchange(eq(apiVeiculosUrl.concat("/marcas/001/modelos")), any(), any(), eq(VeiculosResponse.class)))
        .thenReturn(new ResponseEntity<>(veiculosResponse3, HttpStatus.OK));
    	
    	
    }
	
    @Test
    public void deveRetornarCorolla2020() {
    	ModeloVeiculos modelo = veiculosService.getModelo(MARCA_ID_TOYOTA, MODELO_ID_COROLLA, 2020, null);
    	Assert.assertEquals(modelo2.getAnoModelo(), modelo.getAnoModelo());
    	Assert.assertEquals(modelo2.getDescricaoModelo(), modelo.getDescricaoModelo());
    }
    
    @Test
    public void deveRetornarCorolla9999() {
    	ModeloVeiculos modelo = veiculosService.getModelo(MARCA_ID_TOYOTA, MODELO_ID_COROLLA, null, null);
    	Assert.assertEquals(modelo4.getAnoModelo(), modelo.getAnoModelo());
    	Assert.assertEquals(modelo4.getDescricaoModelo(), modelo.getDescricaoModelo());
    }    
    
    @Test
    public void deveRetornarCorolla9999GetModelo() {
    	ModeloVeiculos modelo = veiculosService.getModeloZeroKm(MARCA_ID_TOYOTA, MODELO_ID_COROLLA, 2022, null);
    	Assert.assertEquals(modelo4.getAnoModelo(), modelo.getAnoModelo());
    	Assert.assertEquals(modelo4.getDescricaoModelo(), modelo.getDescricaoModelo());
    }   
    
    @Test
    public void deveRetornarCorollaDefault() {
    	ModeloVeiculos modelo = veiculosService.getModelo(MARCA_ID_TOYOTA, MODELO_ID_COROLLA, 2022, null);
    	Assert.assertEquals(modelo2.getAnoModelo(), modelo.getAnoModelo());    	
    	Assert.assertEquals(modelo2.getDescricaoModelo(), modelo.getDescricaoModelo());
    }    
    
    @Test
    public void deveRetornarCorollaAnoAtual() {
    	ModeloVeiculos modelo = veiculosService.getModeloZeroKm("001", MODELO_ID_COROLLA, null, null);
    	Integer anoModelo = LocalDate.now().get(ChronoField.YEAR);
    	Assert.assertEquals(anoModelo.toString(), modelo.getAnoModelo());    	
    	Assert.assertEquals(modelo2.getDescricaoModelo(), modelo.getDescricaoModelo());
    }        
    
    @Test
    public void deveRetornarCamry() {
    	ModeloVeiculos modelo = veiculosService.getModelo(MARCA_ID_TOYOTA, MODELO_ID_CAMRY, 2002, null);

    	Assert.assertEquals(modelo1.getDescricaoModelo(), modelo.getDescricaoModelo());
    }
	
    @Test(expected = EntityNotFoundException.class)
    public void deveRetornarErroNehumVeiculoEncontrado() {

        veiculosService.getModelo(MARCA_ID_TOYOTA, "008956", 2020, null);
    }    
    
    @Test(expected = EntityNotFoundException.class)
    public void deveRetornarErroNehumVeiculoEncontrado1() {

        veiculosService.getModeloZeroKm(MARCA_ID_TOYOTA, "008956", 2020, null);
    }       
    
    @Test(expected = ThirdPartyException.class)
    public void deveRetornarErroApiPlanos() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(VeiculosResponse.class)))
                .thenThrow(HttpClientErrorException.BadRequest.class);
        
        veiculosService.getModelo(null, "008956", 2020, null);
    }

}
