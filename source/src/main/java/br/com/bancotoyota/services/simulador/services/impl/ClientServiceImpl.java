package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.StatusClienteResponse;
import br.com.bancotoyota.services.simulador.common.Constants;
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

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
@Slf4j
public class ClientServiceImpl implements ClientService {

    private RestTemplate restTemplate;
    private ApiService apiService;
    private String apiClientesUrl;
    private ObjectMapper mapper;
    private TokenService tokenService;

    @Autowired
    public ClientServiceImpl(RestTemplate restTemplate, ApiService apiService,
                             @Value("${api.clientes.url}") String apiClientesUrl,
                             ObjectMapper mapper, TokenService tokenService) {
        this.restTemplate = restTemplate;
        this.apiClientesUrl = apiClientesUrl;
        this.apiService = apiService;
        this.mapper = mapper;
        this.tokenService = tokenService;
    }

    @Override
    public ValorComprometidoSeguroPrestamista.ClienteComprometimentoSeguro getValoresComprometidos(String cpfCnpj) {
        log.info("Obtendo os valores comprometidos do seguro prestamista do cliente {}...", cpfCnpj);
        try {
            String url = String.format(apiClientesUrl.concat("/%s/valor-comprometido-seguro-prestamista"), cpfCnpj);
            log.info("Endpoint: {}", url);

            ResponseEntity<ValorComprometidoSeguroPrestamista> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(apiService.getAuthorizedHeader(tokenService.generateAccessToken().getToken())), ValorComprometidoSeguroPrestamista.class);
            return responseEntity.getBody().getClienteComprometimentoSeguro();
        } 
        catch (HttpServerErrorException ex) {
        	log.error("Erro ao consultar a api de clientes",ex);
        	if (isServicoForaDoAr(ex)) { 
				throw new ServicoForaException(ex.getMessage(), ex);
			}
			throw new ThirdPartyException(Constants.ERRO_API_CLIENTES, ex);
        }
        catch (RestClientException e) {
        	log.error("Erro ao consultar a api de clientes",e);
            throw new ThirdPartyException(Constants.ERRO_API_CLIENTES, e);
        }
    }

	private boolean isServicoForaDoAr(HttpServerErrorException ex) {
		return ex.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE)
				|| ex.getStatusCode().equals(HttpStatus.GATEWAY_TIMEOUT)
				|| ex.getStatusCode().equals(HttpStatus.INTERNAL_SERVER_ERROR);
	}

    @Override
    public DadosCliente getDadosCliente(String cpfCnpj) {
    	log.info("Obtendo dados do cliente {}...", cpfCnpj);
        String url = String.format(apiClientesUrl.concat("/%s"), cpfCnpj);
        log.info("Endpoint: {}", url);
        
        try {
            ResponseEntity<DadosClienteResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(apiService.getAuthorizedHeader(tokenService.generateAccessToken().getToken())), DadosClienteResponse.class);
            return responseEntity.getBody().getConsultarDadosClienteRes();
        } catch (HttpClientErrorException.BadRequest ex) {
            try {
                ErroExterno erro = mapper.readValue(ex.getResponseBodyAsByteArray(), ErroExterno.class);
                String codigo = erro.getCodigo();
                if (codigo != null && codigo.equals("EN:004")) {
                    return null;
                } else {
                    throw new ThirdPartyException("erro ao chamar  " + url, ex);
                }
            } catch (IOException e) {
                throw new ThirdPartyException("erro ao fazer o parse de mensagem de erro", ex);
            }
        }
        catch (HttpServerErrorException ex) {
        	if (isServicoForaDoAr(ex)) { 
				throw new ServicoForaException(ex.getMessage(), ex);
			}
			throw new ThirdPartyException(Constants.ERRO_API_CLIENTES, ex);
        }
        catch (RestClientException e) {
            throw new ThirdPartyException("erro ao chamar  " + url, e);
        }
    }

    @Override
    public boolean isAtivo(String cpfCnpj) {
        log.info("Verificando se o cliente {} está ativo...", cpfCnpj);
        String url = String.format(apiClientesUrl.concat("/%s/status"), cpfCnpj);
        log.info("Endpoint: {}", url);

        try {
            ResponseEntity<StatusClienteResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(apiService.getAuthorizedHeader(tokenService.generateAccessToken().getToken())), StatusClienteResponse.class);
            boolean ativo = responseEntity.getBody().getClienteStatus().getAtivo();
            log.info("Cliente com CPF/CNPJ {} está {}", cpfCnpj, ativo ? "ativo" : "inativo");
            return ativo;
        } catch (HttpClientErrorException.BadRequest ex) {
            ErroExterno erro = null;
            try {
                erro = mapper.readValue(ex.getResponseBodyAsByteArray(), ErroExterno.class);
            } catch (IOException e) {
                throw new ThirdPartyException("erro ao fazer o parse da resposta da url " + url, ex);
            }
            String codigo = erro.getCodigo();
            if (codigo != null && codigo.equals("EN:002")) {
                return false;
            } else {
                throw new ThirdPartyException("erro ao chamar " + url, ex);
            }
        }
        catch (HttpServerErrorException ex) {
        	if (isServicoForaDoAr(ex)) { 
				throw new ServicoForaException(ex.getMessage(), ex);
			}
			throw new ThirdPartyException(Constants.ERRO_API_CLIENTES, ex);
        }
    }
}
