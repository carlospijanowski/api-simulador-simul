package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.ApiService;
import br.com.bancotoyota.services.simulador.services.PlanoService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import br.com.bancotoyota.services.simulador.utils.ErrorParser;
import br.com.bancotoyota.services.simulador.utils.JsonUtils;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PlanoServiceImpl implements PlanoService {

    public static final String ERRO_NA_API_DE_PLANOS_DE_FINANCIAMENTO = "Erro na API de planos de financiamento";
    public static final String RETORNO = "retorno";
    private RestTemplate restTemplate;
    private ApiService apiService;

    /**
     * Usado para controlar os planos e os retornos disponíveis quando são usados pelo simulador simplificado.
     * Isso significa que quando o simulador simplificado é usado o perfil do usuário é ignorado.
     */
    private String perfilConfiguradoUsuario;

    private String apiPlanosUrl;
    private ErrorParser errorParser;

    private Expression filterExpression;
    private Expression oboFilterExpression;
    
    @Value("${planos.filtro-id-modalidade.enabled:false}")
    private Boolean filtroIdModalidadeEnabled;

    @Autowired
    public PlanoServiceImpl(RestTemplate restTemplate, ApiService apiService,
                            @Value("${simulacao-simplificada.perfil.usuario}") String perfilConfiguradoUsuario,
                            @Value("${api.planos.url}") String apiPlanosUrl,
                            ErrorParser errorParser,
                            @Value("${planos.obo.filter}") String oboFilterExpressionStr,
                            @Value("${planos.filter}") String filterExpressionStr) {
        this.restTemplate = restTemplate;
        this.apiService = apiService;
        this.perfilConfiguradoUsuario = perfilConfiguradoUsuario;
        this.apiPlanosUrl = apiPlanosUrl;
        this.errorParser = errorParser;

        SpelParserConfiguration config = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE,
                this.getClass().getClassLoader());
        ExpressionParser parser = new SpelExpressionParser(config);

        oboFilterExpression = parser.parseExpression(oboFilterExpressionStr);

        filterExpression = parser.parseExpression(filterExpressionStr);
    }

    @Override
    public List<Plano> getPlanos(FiltrosPlano filtrosPlano, String token) {
        log.info("Obtendo os planos de financiamento...");
        try {
            String url = String.format(apiPlanosUrl.concat("/modalidades/%s/marcas/%s?tipo-pessoa=%s&situacao-veiculo=%s" +
                            "&venda-direta=%s&subsidio=%s&ano-modelo=%s&codigo-modelo=%s&regiao=%s"),
                    filtrosPlano.getConfiguracao().getModalidade(), filtrosPlano.getMarca(),
                    filtrosPlano.getConfiguracao().getTipoPessoa(),
                    filtrosPlano.getConfiguracao().getSituacaoVeiculo(),
                    filtrosPlano.getConfiguracao().isVendaDireta(),
                    Boolean.TRUE.equals(filtrosPlano.getConfiguracao().isSubsidio()) ? "SIM" : "NAO",
                    filtrosPlano.getAnoModelo(), filtrosPlano.getModelo(), filtrosPlano.getRegiao());
            log.info("Endpoint: {}", url);

            HttpHeaders headers = apiService.getAuthorizedHeader(token);
            String perfilUsuario = filtrosPlano.getPerfilUsuario();
            if (perfilUsuario == null) {
                perfilUsuario = perfilConfiguradoUsuario;
            }
            
            log.info("Situacao Veiculo: {}",filtrosPlano.getConfiguracao().getSituacaoVeiculo());
            headers.add("perfil", perfilUsuario);
            headers.add("cnpj-origem-negocio", filtrosPlano.getCnpjOrigemNegocio());
            // workaround para formato de resposda incorreto que ainda está habilitado para o OBO
            headers.add("respostaCorreta","true");

            ResponseEntity<PlanosResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), PlanosResponse.class);
            List<Plano> planos = responseEntity.getBody().getResponse().getPlanos();
            log.info("Foram encontrados {} planos", planos.size());
            return planos;
        } catch (HttpClientErrorException.NotFound e) {
            if (JsonUtils.canConvert(e.getResponseBodyAsString(), new TypeReference<List<MessageHolder>>(){})) {
                log.error(String.format("Não foram encontrados planos de financiamento para a modalidade '%s', " +
                                "tipo pessoa '%s', marca '%s', modelo '%s' '%s' ano '%s', %s venda direta, %s subsídio e " +
                                "origem negócio com CNPJ '%s' na região '%s", filtrosPlano.getConfiguracao().getModalidade(),
                        filtrosPlano.getConfiguracao().getTipoPessoa(), filtrosPlano.getMarca(), filtrosPlano.getModelo(),
                        filtrosPlano.getConfiguracao().getSituacaoVeiculo(), filtrosPlano.getAnoModelo(),
                        filtrosPlano.getConfiguracao().isVendaDireta() ? "com" : "sem",
                        filtrosPlano.getConfiguracao().isSubsidio() ? "com" : "sem",
                        filtrosPlano.getCnpjOrigemNegocio(), filtrosPlano.getRegiao()));
                throw new EntityNotFoundException(Plano.class, "plano-financiamento");
            }
            throw new ThirdPartyException(ERRO_NA_API_DE_PLANOS_DE_FINANCIAMENTO, e);
        } catch (HttpStatusCodeException e) {
            throw new ThirdPartyException(ERRO_NA_API_DE_PLANOS_DE_FINANCIAMENTO, e);
        }
    }

    @Override
    public Plano getPlanoCicloToyota(BigDecimal valorBem, BigDecimal valorEntrada, FiltrosPlano filtrosPlano, String token) {
        List<Plano> planos = getPlanos(filtrosPlano, token);

        // TODO: o resta um deveria selecionar um plano que possa ser usado na data de vencimento da proposta atual

        BigDecimal percentualEntrada = valorEntrada.multiply(NumberUtils.CEM).divide(valorBem,
                new MathContext(2, RoundingMode.HALF_UP));

        planos = filtrarIdModalidade(filtrosPlano.getConfiguracao().getModalidade(), filtrosPlano.getIdsModalidade(), planos);
        planos = filtrarPlanos(planos, percentualEntrada, filtrosPlano.isWorkaroundObo(), filtrosPlano.getConfiguracao().getModalidade());
        planos = workaroundObo(filtrosPlano.isWorkaroundObo(), planos);
        
        
        if (planos.size() > 1) {
            Optional<LocalDateTime> optionalLocalDateTime = planos.stream().map(Plano::getDataInicialVigencia)
                    .max(Comparator.naturalOrder());
            if (optionalLocalDateTime.isPresent()) {
                // TODO: adicionar número máximo de parcelas
            	//Ordena a lista pelo maior valor de parcela minima de entrada para o menor.
                planos = planos.stream().filter(p -> p.getDataInicialVigencia().equals(optionalLocalDateTime.get()))
                        .sorted((p1, p2) -> p2.getParcelaMinimaEntrada().compareTo(p1.getParcelaMinimaEntrada()))
                        .collect(Collectors.toList());
            }
        }

        return planos.stream().findFirst()
                .orElseThrow(() -> new EntityNotFoundException(Plano.class, "plano-ciclo-toyota"));
    }

    private List<Plano> workaroundObo(boolean isWorkaroundObo, List<Plano> planos) {
        if (isWorkaroundObo && !planos.isEmpty()) {
        	// No método anterior a lista de planos foi filtrada para conter somente os planos sem subsidio e/ou com subsidio fixo, 
        	// caso exista algum plano subsidiado (restariam somente os planos com subsidio fixo), os planos subsidiados se tornam prioritários, 
        	// pois normalmente as taxas são melhores para o cliente.
            List<Plano> planosSubsidiados = planos.stream().filter(p -> Boolean.TRUE.equals(p.getPlanoSubsidiado()))
                    .collect(Collectors.toList());
            if (!planosSubsidiados.isEmpty()) {
                planos = planosSubsidiados;
            }
        }
        return planos;
    }
    
    private List<Plano> filtrarIdModalidade(Modalidade modalidade, List<String> idsModalidade, List<Plano> planos) {
        if (filtroIdModalidadeEnabled && !planos.isEmpty()) {
        	/*
        	 * O filtro por idModalidade foi implementado para filtrar os planos de produtos, que poder ser da modalidade (Ciclo ou CDC) mas com id-modalidade diferentes
        	 */
        	List<Plano> planosIdModalidade = new ArrayList<>();
        	String regexRemoveZeroEsquerda = "^0+(?!$)";
        	if (idsModalidade != null && !idsModalidade.isEmpty()) {
        		for (String idModalidade : idsModalidade) {
        			planosIdModalidade.addAll(planos.stream().filter(p -> p.getIdModalidade() != null && p.getIdModalidade().replaceFirst(regexRemoveZeroEsquerda, "").equals(idModalidade)).collect(Collectors.toList()));
				}
        		planos = planosIdModalidade;	
        	}else if (modalidade.equals(Modalidade.CDC)) {
        		planos = planos.stream().filter(p -> p.getIdModalidade() != null && p.getIdModalidade().replaceFirst(regexRemoveZeroEsquerda, "").equals("1")).collect(Collectors.toList());
        	}else if (modalidade.equals(Modalidade.CICLO_TOYOTA)) {
        		planos = planos.stream().filter(p -> p.getIdModalidade() != null && p.getIdModalidade().replaceFirst(regexRemoveZeroEsquerda, "").equals("12")).collect(Collectors.toList());
        	}
        	
        	
        	
        }
        return planos;
    }    

    private List<Plano> filtrarPlanos(List<Plano> planos, BigDecimal percentualEntrada, boolean isWorkaroundObo, Modalidade modalidade) {

        /* Incluído em: 24/05/2021
         * Autor: Leandro Haruo Aoyagi
         * Necessidade: Os calculos de parcela desejada passaram a verificar se o plano permite parcela balão para os novos produtos Ciclo One e Ciclo Privilege,
         * 	dessa forma, para não impactar as simulações simplificadas e alternativa, temos que remover da lista, os planos que não permitem parcela balão.
         */
    	if (modalidade.equals(Modalidade.CICLO_TOYOTA)) {
        planos = planos.stream()
                .filter(p -> p.getPermiteBalao())
                .collect(Collectors.toList());
    	}
    	
        planos = planos.stream().filter(p -> filtrarPorDescricao(p, isWorkaroundObo)).collect(Collectors.toList());
        
        // filtrar pela entrada
        planos = planos.stream()
                .filter(p -> p.getParcelaMinimaEntrada().compareTo(percentualEntrada) <= 0)
                .collect(Collectors.toList());

        if (isWorkaroundObo) {
        	// Filtra os planos sem subsídio e os planos com subsídio fixo, eliminando somente os planos de subsídio variável (onde o vendedor insere o subsidio), 
        	// pois as simulações simplificadas não possuem valores de subsidio na entrada.
            planos = planos.stream()
                    .filter(p -> p.getTipoSubsidio() == null || p.getTipoSubsidio().isEmpty() || p.getTipoSubsidio().equals("F"))
                    .collect(Collectors.toList());
        }

        return planos;
    }

    /**
     * Talvez tenhamos que fazer a aplicação desse filtro dinamicamente usando um avaliador de expressões a partir de uma
     * expressão configurável.
     */
    private boolean filtrarPorDescricao(Plano plano, boolean isWorkaroundObo) {
        if (isWorkaroundObo) {
            return oboFilterExpression.getValue(plano, Boolean.class);
        } else {
            return filterExpression.getValue(plano, Boolean.class);
        }
    }

    @Override
    public List<Retorno> getRetornos(String codigoPlano, String cnpjOrigemNegocio, String perfilUsuario, String token) {
        log.info("Obtendo os retornos do plano de financiamento {}...", codigoPlano);
        try {
            String url = String.format(apiPlanosUrl.concat("/%s/codigos-simulacao"), codigoPlano);
            log.info("Endpoint: {}", url);

            HttpHeaders headers = apiService.getAuthorizedHeader(token);
            if (perfilUsuario == null) {
                perfilUsuario = perfilConfiguradoUsuario;
            }
            headers.add("perfil", perfilUsuario);
            headers.add("cnpj-origem-negocio", cnpjOrigemNegocio);

            ResponseEntity<RetornoResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), RetornoResponse.class);
            List<Retorno> retornos = responseEntity.getBody().getResponse().getRetornos();
            log.info("Foram encontrados {} retornos", retornos.size());
            return retornos;
        } catch (HttpClientErrorException.NotFound e) {
            if (JsonUtils.canConvert(e.getResponseBodyAsString(), new TypeReference<List<MessageHolder>>(){})) {
                log.error(String.format("Não foram encontrados retornos para o plano com código '%s' " +
                        "e origem negócio com CNPJ '%s', ", codigoPlano, cnpjOrigemNegocio));
                throw new EntityNotFoundException(Retorno.class, RETORNO);
            }
            throw new ThirdPartyException(ERRO_NA_API_DE_PLANOS_DE_FINANCIAMENTO, e);
        } catch (HttpStatusCodeException e) {
            throw new ThirdPartyException(ERRO_NA_API_DE_PLANOS_DE_FINANCIAMENTO, e);
        }
    }

    @Override
    public Retorno getRetorno(String codigoPlano, String cnpjOrigemNegocio, String perfilUsuario, String token) {
        List<Retorno> retornos = getRetornos(codigoPlano, cnpjOrigemNegocio, perfilUsuario, token);
        Optional<Retorno> retornoDefault = retornos.stream()
                .filter(retorno -> Constants.RETORNO_DEFAULT.equals(retorno.getCodigo()))
                .findFirst();
        if (retornoDefault.isPresent()) {
            return retornoDefault.get();
        }
        return retornos.stream()
                .sorted(Comparator.comparing(Retorno::getCodigo)).findFirst()
                .orElseThrow(() -> new EntityNotFoundException(Retorno.class, RETORNO));
    }
    
    @Override
    public Retorno getRetorno(String codigoPlano, String cnpjOrigemNegocio, String perfilUsuario, String codigoRetorno, String token) {
        List<Retorno> retornos = getRetornos(codigoPlano, cnpjOrigemNegocio, perfilUsuario, token);
        return retornos.stream()
                .filter(r -> r.getCodigo().equals(codigoRetorno))
                .findFirst().orElseThrow(() -> new EntityNotFoundException(Retorno.class, RETORNO));
    }    

    @Override
    public List<Prazo> findPrazos(Integer planoId, Collection<Integer> meses) {
        //
        String url = apiPlanosUrl + "/" + planoId + "/prazos";
        ParameterizedTypeReference<List<Prazo>> responseType = new ParameterizedTypeReference<List<Prazo>>() {};

        try {
            // esse request não deve passar pelo gateway para que seja o mais rápido possível
            ResponseEntity<List<Prazo>> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
                    null, responseType);
            List<Prazo> lista = responseEntity.getBody();
            if (meses != null && !meses.isEmpty()) {
                return lista.stream().filter(p -> meses.contains(p.getNumeroDeParcelas())).collect(Collectors.toList());
            }
            return lista;
        } catch (HttpClientErrorException.NotFound ex) {
            log.error("não encontrado: " + ex.getResponseBodyAsString());
            List<Erro> l = errorParser.parseList(ex);
            if (!l.isEmpty()) {
                throw new EntityNotFoundException(l.iterator().next());
            } else {
                throw new EntityNotFoundException(Prazo.class, planoId.toString());
            }
        }
    }

    @Override
    public Subsidio findSubsidio(Integer planoId) {
        if (planoId == -1) {
            return null;
        }

        String url = apiPlanosUrl + "/" + planoId + "/subsidio";
        ParameterizedTypeReference<Subsidio> responseType = new ParameterizedTypeReference<Subsidio>() {};

        // esse request retorna null se não for encontrado subsídio para o plano
        ResponseEntity<Subsidio> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
                null, responseType);

        // BTO-3577-Bugfix: Quando há subsídio, ordena suas faixas de valor por ordem crescente de valor mínimo.
        Subsidio subsidio = responseEntity.getBody();

        if (subsidio != null && subsidio.getFaixasValor() != null && !subsidio.getFaixasValor().isEmpty()) {
            List faixasValorOrdenadas = subsidio.getFaixasValor().stream().sorted(
                    Comparator.comparing(Subsidio.FaixaValor::getValorInicial)
                    ).collect(Collectors.toList());
            subsidio.setFaixasValor(faixasValorOrdenadas);
        }

        return subsidio;
    }

    @Override
    public Plano getPlanoById(Integer planoId) {
        String url = apiPlanosUrl + "/" + planoId;
        ParameterizedTypeReference<PlanoResponse> responseType = new ParameterizedTypeReference<PlanoResponse>() {};
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("respostaCorreta", "true");
        try {
            ResponseEntity<PlanoResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
                    null, responseType);
            return responseEntity.getBody().getPlano();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new EntityNotFoundException(new Erro(errorParser.parseMessage(ex.getResponseBodyAsString())));
        }
    }
}
