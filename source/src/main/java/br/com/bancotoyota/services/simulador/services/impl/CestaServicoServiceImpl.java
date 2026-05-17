package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.ApiService;
import br.com.bancotoyota.services.simulador.services.CestaServicoService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import br.com.bancotoyota.services.simulador.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class CestaServicoServiceImpl implements CestaServicoService {

    public static final String ERRO_NA_API_DE_CESTAS_DE_SERVICOS = "Erro na API de cestas de serviços";
    private RestTemplate restTemplate;
    private ApiService apiService;
    private String perfilConfiguradoUsuario;
    private String apiCestasServivosUrl;

    @Autowired
    public CestaServicoServiceImpl(RestTemplate restTemplate, ApiService apiService,
                                   @Value("${simulacao-simplificada.perfil.usuario}") String perfilConfiguradoUsuario,
                                   @Value("${api.cestas-servicos.url}") String apiCestasServivosUrl) {
        this.restTemplate = restTemplate;
        this.apiService = apiService;
        this.perfilConfiguradoUsuario = perfilConfiguradoUsuario;
        this.apiCestasServivosUrl = apiCestasServivosUrl;
    }

    @Override
    public List<CestaServico> getCestasServicos(String cnpj, Modalidade modalidade, TipoPessoa tipoPessoa,
                                                String perfilUsuario, String token) {
        log.info("Obtendo as cestas de serviços da loja {}, modalidade {} e tipo pessoa {}...", cnpj, modalidade, tipoPessoa);
        try {
            String url = String.format(apiCestasServivosUrl.concat("/origem-negocio/%s?modalidade=%s&tipo-pessoa=%s"),
                    cnpj, modalidade, tipoPessoa);
            log.info("Endpoint: {}", url);
            HttpHeaders headers = apiService.getAuthorizedHeader(token);
            headers.add("perfil", perfilConfiguradoUsuario);

            ResponseEntity<CestaServicoResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), CestaServicoResponse.class);
            List<CestaServico> cestasServicos = responseEntity.getBody().getResponse().getItens();
            log.info("Foram encontradas {} cestas de serviços", cestasServicos.size());
            return cestasServicos;
        } catch (HttpClientErrorException.NotFound e) {
            if (JsonUtils.canConvert(e.getResponseBodyAsString(), MessageHolder.class)) {
                log.error(String.format("Não foram encontradas cestas de serviço para a origem negócio com CNPJ '%s', " +
                        "modalidade '%s' e tipo pessoa '%s'", cnpj, modalidade, tipoPessoa));
                throw new EntityNotFoundException(CestaServico.class, "cesta-servico");
            }
            throw new ThirdPartyException(ERRO_NA_API_DE_CESTAS_DE_SERVICOS, e);
        } catch (HttpStatusCodeException e) {
            throw new ThirdPartyException(ERRO_NA_API_DE_CESTAS_DE_SERVICOS, e);
        }
    }

    @Override
    public CestaServico getCestaServicoMaisCara(String cnpj, Modalidade modalidade, TipoPessoa tipoPessoa,
            String perfilUsuario, String token) {
        List<CestaServico> cestasServicos = getCestasServicos(cnpj, modalidade, tipoPessoa, perfilUsuario, token);
        return cestasServicos.stream().sorted((o1, o2) -> o2.getValor().compareTo(o1.getValor()))
                .findFirst().orElseThrow(() -> new EntityNotFoundException(CestaServico.class, "cesta-servico-mais-cara"));
    }

    @Override
    public List<CestaItem> getDespesas(Integer codigoCesta, String token) {
        log.info("Obtendo as despesas da cesta de serviço {}...", codigoCesta);
        try {
            String url = String.format(apiCestasServivosUrl.concat("/%s"), codigoCesta);
            log.info("Endpoint: {}", url);

            ResponseEntity<DespesaResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(apiService.getAuthorizedHeader(token)), DespesaResponse.class);
            List<CestaItem> cestaItems = responseEntity.getBody().getResponse().getItens();
            log.info("Foram encontradas {} despesas", cestaItems.size());
            return cestaItems;
        } catch (HttpClientErrorException.NotFound e) {
            if (JsonUtils.canConvert(e.getResponseBodyAsString(), MessageHolder.class)) {
                log.error(String.format("Não foram encontradas despesas paraa cesta de serviço com código '%s'",
                        codigoCesta));
                throw new EntityNotFoundException(CestaItem.class, "despesa");
            }
            throw new ThirdPartyException(ERRO_NA_API_DE_CESTAS_DE_SERVICOS, e);
        } catch (HttpStatusCodeException e) {
            throw new ThirdPartyException(ERRO_NA_API_DE_CESTAS_DE_SERVICOS, e);
        }
    }
}
