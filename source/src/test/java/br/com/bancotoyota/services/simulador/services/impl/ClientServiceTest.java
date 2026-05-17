package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.AccessToken;
import br.com.bancotoyota.services.simulador.beans.StatusClienteResponse;
import br.com.bancotoyota.services.simulador.entities.DadosCliente;
import br.com.bancotoyota.services.simulador.entities.DadosClienteResponse;
import br.com.bancotoyota.services.simulador.entities.ErroExterno;
import br.com.bancotoyota.services.simulador.entities.ValorComprometidoSeguroPrestamista;
import br.com.bancotoyota.services.simulador.services.ApiService;
import br.com.bancotoyota.services.simulador.services.ClientService;
import br.com.bancotoyota.services.simulador.services.TokenService;
import br.com.bancotoyota.services.simulador.services.exceptions.ServicoForaException;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "/application.properties")
public class ClientServiceTest {

    @Value("${api.clientes.url}")
    private String apiClientesUrl;

    private ClientService clientService;
    private ValorComprometidoSeguroPrestamista.ClienteComprometimentoSeguro clienteComprometimentoSeguro =
            new ValorComprometidoSeguroPrestamista.ClienteComprometimentoSeguro(BigDecimal.ZERO, BigDecimal.ZERO);
    private DadosCliente dadosCliente = new DadosCliente("08/04/2984");
    private StatusClienteResponse.ClienteStatus clienteStatus = new StatusClienteResponse.ClienteStatus(true, null);
    
    @MockBean
    private TokenService tokenService;
    
    private AccessToken token;

    @MockBean
    private RestTemplate restTemplate;

    @Before
    public void setup() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        clientService = new ClientServiceImpl(restTemplate, new ApiService(), apiClientesUrl, mapper, tokenService);

        when(restTemplate.exchange(eq(apiClientesUrl.concat("/18919846081/valor-comprometido-seguro-prestamista")),
                any(), any(), eq(ValorComprometidoSeguroPrestamista.class)))
                .thenReturn(new ResponseEntity<>(new ValorComprometidoSeguroPrestamista(clienteComprometimentoSeguro), HttpStatus.OK));

        when(restTemplate.exchange(eq(apiClientesUrl.concat("/17949665874")), any(), any(), eq(DadosClienteResponse.class)))
                .thenReturn(new ResponseEntity<>(new DadosClienteResponse(dadosCliente), HttpStatus.OK));

        ErroExterno erro = new ErroExterno("EN:004", "Cliente não encontrado");

        when(restTemplate.exchange(eq(apiClientesUrl.concat("/98765432100")), any(), any(), eq(DadosClienteResponse.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "", new HttpHeaders(),
                        mapper.writeValueAsBytes(erro), Charset.defaultCharset()));

        when(restTemplate.exchange(eq(apiClientesUrl.concat("/42111195839/status")),
                any(), any(), eq(StatusClienteResponse.class)))
                .thenReturn(new ResponseEntity<>(new StatusClienteResponse(clienteStatus), HttpStatus.OK));
        
        token = new AccessToken();
        token.setToken("Token");
        when(tokenService.generateAccessToken()).thenReturn(token);
        
    }

    @Test
    public void deveRetornarValoresComprometidos() {
        assertEquals(clienteComprometimentoSeguro, clientService.getValoresComprometidos("18919846081"));
    }

    @Test(expected = ThirdPartyException.class)
    public void deveRetornarErroApiCliente() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(ValorComprometidoSeguroPrestamista.class)))
                .thenThrow(HttpClientErrorException.BadRequest.class);
        clientService.getValoresComprometidos(null);
    }
    
    @Test(expected = ServicoForaException.class)
    public void deveRetornarServicoForaApiClienteIndisponivel() {
    	HttpServerErrorException ex = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE); 
        when(restTemplate.exchange(anyString(), any(), any(), eq(ValorComprometidoSeguroPrestamista.class)))
                .thenThrow(ex);
        clientService.getValoresComprometidos(null);
    }
    
    @Test(expected = ServicoForaException.class)
    public void deveRetornarServicoForaApiClienteIndisponivelComInternalServerError() {
    	HttpServerErrorException ex = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR); 
        when(restTemplate.exchange(anyString(), any(), any(), eq(ValorComprometidoSeguroPrestamista.class)))
                .thenThrow(ex);
        clientService.getValoresComprometidos(null);
    }
    
    @Test(expected = ServicoForaException.class)
    public void deveRetornarServicoForaApiClienteTimeout() {
    	HttpServerErrorException ex = new HttpServerErrorException(HttpStatus.GATEWAY_TIMEOUT); 
        when(restTemplate.exchange(anyString(), any(), any(), eq(ValorComprometidoSeguroPrestamista.class)))
                .thenThrow(ex);
        clientService.getValoresComprometidos(null);
    }
    
    @Test(expected = ThirdPartyException.class)
    public void deveRetornarThirdPartyException() {
    	HttpServerErrorException ex = new HttpServerErrorException(HttpStatus.INSUFFICIENT_STORAGE); 
        when(restTemplate.exchange(anyString(), any(), any(), eq(ValorComprometidoSeguroPrestamista.class)))
                .thenThrow(ex);
        clientService.getValoresComprometidos(null);
    }

    @Test(expected = ServicoForaException.class)
    public void naodeveRetornarDadosClienteQuandoServicoEstiverIndisponivel() {
    	HttpServerErrorException error = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
    	when(restTemplate.exchange(eq(apiClientesUrl.concat("/11047181767")), any(), any(), eq(DadosClienteResponse.class)))
        .thenThrow(error);
        assertEquals(dadosCliente, clientService.getDadosCliente("11047181767"));
    }
    
    @Test(expected = ServicoForaException.class)
    public void naodeveRetornarDadosClienteQuandoServicoEstiverDandoTimeout() {
    	HttpServerErrorException error = new HttpServerErrorException(HttpStatus.GATEWAY_TIMEOUT);
    	when(restTemplate.exchange(eq(apiClientesUrl.concat("/11047181767")), any(), any(), eq(DadosClienteResponse.class)))
        .thenThrow(error);
        assertEquals(dadosCliente, clientService.getDadosCliente("11047181767"));
    }
    
    @Test(expected = ThirdPartyException.class)
    public void naodeveRetornarDadosClienteQuandoServicoDerErro500() {
    	HttpServerErrorException error = new HttpServerErrorException(HttpStatus.INSUFFICIENT_STORAGE);
    	when(restTemplate.exchange(eq(apiClientesUrl.concat("/11047181767")), any(), any(), eq(DadosClienteResponse.class)))
        .thenThrow(error);
        assertEquals(dadosCliente, clientService.getDadosCliente("11047181767"));
    }
    
    
    @Test
    public void naoDeveRetornarDadosClienteQuandoApiEstiverIndisponivel() {
        assertEquals(dadosCliente, clientService.getDadosCliente("17949665874"));
    }

    @Test
    public void deveRetornarNenhumCliente() {
        assertNull(clientService.getDadosCliente("98765432100"));
    }
    
    @Test(expected = ServicoForaException.class)
    public void deveRetornarNenhumClienteQuandoApiIndisponivel() {
    	HttpServerErrorException error = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
    	when(restTemplate.exchange(eq(apiClientesUrl.concat("/42111195830/status")),
                any(), any(), eq(StatusClienteResponse.class)))
                .thenThrow(error);
        assertNull(clientService.isAtivo("42111195830"));
    }
    
    @Test(expected = ServicoForaException.class)
    public void deveRetornarNenhumClienteQuandoApiEstiverComErroTimeout() {
    	HttpServerErrorException error = new HttpServerErrorException(HttpStatus.GATEWAY_TIMEOUT);
    	when(restTemplate.exchange(eq(apiClientesUrl.concat("/42111195830/status")),
                any(), any(), eq(StatusClienteResponse.class)))
                .thenThrow(error);
        assertNull(clientService.isAtivo("42111195830"));
    }
    
    @Test(expected = ThirdPartyException.class)
    public void deveRetornarNenhumClienteQuandoApiEstiverComErroInterno() {
    	HttpServerErrorException error = new HttpServerErrorException(HttpStatus.INSUFFICIENT_STORAGE);
    	when(restTemplate.exchange(eq(apiClientesUrl.concat("/42111195830/status")),
                any(), any(), eq(StatusClienteResponse.class)))
                .thenThrow(error);
        assertNull(clientService.isAtivo("42111195830"));
    }

    @Test
    public void deveRetornarClienteAtivo() {
        assertTrue(clientService.isAtivo("42111195839"));
    }
}