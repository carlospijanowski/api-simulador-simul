package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.request.FatorSimulacao;
import br.com.bancotoyota.services.simulador.beans.request.FatorTaxaDetalhe;
import br.com.bancotoyota.services.simulador.beans.response.*;
import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.entities.Modalidade;
import br.com.bancotoyota.services.simulador.entities.Taxa;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.ApiService;
import br.com.bancotoyota.services.simulador.services.MotorTaxaPlanoService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import br.com.bancotoyota.services.simulador.utils.ErrorParser;
import br.com.bancotoyota.services.simulador.utils.JsonUtils;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MotorTaxaPlanoServiceImpl implements MotorTaxaPlanoService {

    public static final String ERRO_NA_API_DE_PLANOS_DE_FINANCIAMENTO = "Erro na API de planos de financiamento";
    public static final String RETORNO = "retorno";

    private RestTemplate restTemplate;
    private ApiService apiService;

    /**
     * Usado para controlar os planos e os retornos disponíveis quando são usados pelo simulador simplificado.
     * Isso significa que quando o simulador simplificado é usado o perfil do usuário é ignorado.
     */
    private String apiMotorTaxaUrl;
    private ErrorParser errorParser;

    private Expression filterExpression;
    private Expression oboFilterExpression;
    private boolean filtroIdModalidadeEnabled;

    /**
     * Usado para controlar os planos e os retornos disponíveis quando são usados pelo simulador simplificado.
     * Isso significa que quando o simulador simplificado é usado o perfil do usuário é ignorado.
     */
    private String perfilConfiguradoUsuario;

    private FatorSimulacao fatorSimulacao;

    @Autowired
    public MotorTaxaPlanoServiceImpl(RestTemplate restTemplate, ApiService apiService,
                                     @Value("${simulacao-simplificada.perfil.usuario}") String perfilConfiguradoUsuario,
                                     @Value("${api.motor-taxa.url}") String apiMotorTaxaUrl,
                                     ErrorParser errorParser,
                                     @Value("${planos.obo.filter}") String oboFilterExpressionStr,
                                     @Value("${planos.filter}") String filterExpressionStr,
                                     @Value("${planos.filtro-id-modalidade.enabled:false}") boolean filtroIdModalidadeEnabled,
                                     FatorSimulacao fatorSimulacao
    ) {
        this.restTemplate = restTemplate;
        this.apiService = apiService;
        this.apiMotorTaxaUrl = apiMotorTaxaUrl;
        this.errorParser = errorParser;

        SpelParserConfiguration config = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE,
                this.getClass().getClassLoader());
        ExpressionParser parser = new SpelExpressionParser(config);

        oboFilterExpression = parser.parseExpression(oboFilterExpressionStr);

        filterExpression = parser.parseExpression(filterExpressionStr);
        this.filtroIdModalidadeEnabled = filtroIdModalidadeEnabled;
        this.perfilConfiguradoUsuario = perfilConfiguradoUsuario;
        this.fatorSimulacao = fatorSimulacao;
    }

    @Override
    public List<Retorno> getRetornos(String codigoPlano, String cnpjOrigemNegocio, String perfilUsuario, String planoVersao) {
        log.info("Obtendo os retornos do plano de financiamento {}...", codigoPlano);
        try {
            String url = String.format(apiMotorTaxaUrl.concat("/plano/%s/codigos-simulacao"), codigoPlano);
            log.info("Endpoint: {}", url);

            HttpHeaders headers = apiService.getHeader();
            if (perfilUsuario == null) {
                perfilUsuario = perfilConfiguradoUsuario;
            }
            headers.add("perfil", perfilUsuario);
            headers.add("cnpj-origem-negocio", cnpjOrigemNegocio);
            headers.add("plano-versao", planoVersao);

            ResponseEntity<RetornoMotorTaxaResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), RetornoMotorTaxaResponse.class);
            List<Retorno> retornos = responseEntity.getBody().getRetornos();
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
    public Retorno getRetorno(String codigoPlano, String cnpjOrigemNegocio, String perfilUsuario) {
        String planoVersao="";
        List<Retorno> retornos = getRetornos(codigoPlano, cnpjOrigemNegocio, perfilUsuario, planoVersao);
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
    public Retorno getRetorno(String codigoPlano, String cnpjOrigemNegocio, String perfilUsuario, String codigoRetorno) {
        String planoVersao="";
        List<Retorno> retornos = getRetornos(codigoPlano, cnpjOrigemNegocio, perfilUsuario, planoVersao);
        return retornos.stream()
                .filter(r -> r.getCodigo().equals(codigoRetorno))
                .findFirst().orElseThrow(() -> new EntityNotFoundException(Retorno.class, RETORNO));
    }

    @Override
    public List<Prazo> findPrazos(Integer planoId, Collection<Integer> meses) {
        Taxa taxas = this.findTaxas(planoId);
        getFator(taxas);

        // Conversão do dado retornado para o formato de Prazos já conhecido
        List<Prazo> prazos = convertTaxastoPrazos(taxas);
        if (meses != null && !meses.isEmpty()) {
            fatorSimulacao.setFatorTaxaDetalhes(fatorSimulacao.getFatorTaxaDetalhes().stream().filter(t -> meses.contains(t.getParcela())).collect(Collectors.toSet()));
            return prazos.stream().filter(p -> meses.contains(p.getNumeroDeParcelas())).collect(Collectors.toList());
        }
        return prazos;
    }

    private void applyFator(BigDecimal fator, Taxa taxas) {
        AtomicReference<FatorTaxaDetalhe> fatorTaxaDetalhe = new AtomicReference<>(new FatorTaxaDetalhe());
        taxas.getTaxasDetalhe().forEach(t -> {
            fatorTaxaDetalhe.get().setValorTaxaBancoOriginal(t.getTaxaMes());
            if (fator != null) {
                t.setTaxaMes(t.getTaxaMes().add(fator));
                fatorTaxaDetalhe.get().setFator(fator.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP));
            }
            fatorTaxaDetalhe.get().setValorTaxaEfetiva(t.getTaxaMes());
            fatorTaxaDetalhe.get().setParcela(t.getPrazoMinimo());
            fatorSimulacao.getFatorTaxaDetalhes().add(fatorTaxaDetalhe.get());
            fatorTaxaDetalhe.set(new FatorTaxaDetalhe());
        });
    }

    private void getFator(Taxa taxas) {
        BigDecimal fator = null;
        if("DIRECT".equalsIgnoreCase(fatorSimulacao.getOrigemSolicitacao()) &&
                Objects.nonNull(fatorSimulacao.getRating())) {
            String fatorStr = this.findFator(FatorRequest.builder()
                    .planoId(fatorSimulacao.getPlanoId())
                    .cnpjOrigemNegocio(fatorSimulacao.getCnpjOrigemNegocio())
                    .rating(fatorSimulacao.getRating()).build());
            if(StringUtils.isNotEmpty(fatorStr)) {
                fator = new BigDecimal(fatorStr).divide(BigDecimal.valueOf(100));
                applyFator(fator, taxas);
            }
        }
    }

    public void applyFatorRating(SimulacaoParcResidualResponse simulacaoParcResidualResponse) {
        List<FatorTaxaDetalhe> fatorTaxaDetalhe = new ArrayList<>(fatorSimulacao.getFatorTaxaDetalhes());
        simulacaoParcResidualResponse.getPrazos().forEach(s -> fatorTaxaDetalhe.stream()
                .filter(f -> f.getParcela().equals(s.getParcelas()) && f.getValorTaxaEfetiva().equals(s.getValorTaxaBanco()))
                .findFirst()
                .ifPresent(f -> {
                    s.setFatorRating(FatorRating.builder()
                            .fator(f.getFator())
                            .valorTaxaBancoOriginal(f.getValorTaxaBancoOriginal())
                            .build());
                }));
    }

    @Override
    public PlanoMotorTaxa getPlanoPorId(Integer planoId) {
        String url = apiMotorTaxaUrl + "/plano/" + planoId;
        ParameterizedTypeReference<PlanoMotorTaxaResponse> responseType = new ParameterizedTypeReference<PlanoMotorTaxaResponse>() {};

        try {
            // esse request não deve passar pelo gateway para que seja o mais rápido possível
            ResponseEntity<PlanoMotorTaxaResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
                    null, responseType);
            PlanoMotorTaxa planoResponse = responseEntity.getBody().getPlano();
            return planoResponse;
        } catch (HttpClientErrorException.NotFound ex) {
            log.error("não encontrado: " + ex.getResponseBodyAsString());
            List<Erro> l = errorParser.parseList(ex);
            if (!l.isEmpty()) {
                throw new EntityNotFoundException(l.iterator().next());
            } else {
                throw new EntityNotFoundException(PlanoMotorTaxa.class, planoId.toString());
            }
        }

    }

    @Override
    public Plano convertPlanoFromMotorTaxa(PlanoMotorTaxa planoResponse) {
        return Plano.builder()
                .codigoModalidade(planoResponse.getDadosExternos().getCodigoModalidade().getValue())
                .codigoPlano(String.valueOf(planoResponse.getPlanoId()))
                .codigoBasePlano(String.valueOf(planoResponse.getDadosExternos().getCodigoBasePlano()))
                .descricao(planoResponse.getNome())
                .anoModeloInicio(null) //Não está sendo utilizado
                .anoModeloFinal(null) //Não está sendo utilizado
                .dataInicialVigencia(planoResponse.getDataInicialVigencia().atTime(00,00))
                //FinalVigencia?
                //PlanoBloqueado?
                .parcelaMinimaEntrada(planoResponse.getParcelaMinimaEntrada())
                .parcelaMaximaEntrada(planoResponse.getParcelaMaximaEntrada())
                .parcelaMinimaBalao(planoResponse.getParcelasIntermediarias() != null ? planoResponse.getParcelasIntermediarias().getPercentualMinimoBalao() : null)
                .parcelaMaximaBalao(planoResponse.getParcelasIntermediarias() != null ? planoResponse.getParcelasIntermediarias().getPercentualMaximoBalao() : null)
                .quantidadeMinimaCarencia(planoResponse.getQuantidadeMinimaCarencia())
                .quantidadeMaximaCarencia(planoResponse.getQuantidadeMaximaCarencia())
                //O TIPO DO PLANO ANTIGO NÃO É O MESMO TIPO DO PLANO NOVO (CDC-TCM ou "CDC-TCM-SUBSIDIO")
                //FOI FEITA A MESMA REGRA QUE ERA FEITA NA API PLANOS V2
                .tipoPlano(planoResponse.getPlanoSubsidiado() ? "CDC-TCM-SUBSIDIO" : "CDC-TCM")
                .planoSubsidiado(planoResponse.getPlanoSubsidiado())
                .tipoSubsidio(planoResponse.getSubsidio() != null && planoResponse.getSubsidio().getTipoSubsidio() != null ? planoResponse.getSubsidio().getTipoSubsidio().getKey() : null) //VALUE?
                .primeiroVencimentoFixo(planoResponse.getPrimeiroVencimento() != null && planoResponse.getPrimeiroVencimento().getPrimeiroVencimentoFixo() != null ? planoResponse.getPrimeiroVencimento().getPrimeiroVencimentoFixo() : false)
                .indicadorFaixaAno(planoResponse.getDadosExternos().getIndicadorFaixaAno())
                .prazoValidade(planoResponse.getPrazoValidade())
                .idModalidade(planoResponse.getDadosExternos().getIdModalidade())
                .isentaIof(BooleanUtils.isTrue(planoResponse.getIsentaIof()) ? "S" : "N")
                .periodicidade(planoResponse.getListaPeriodicidades() != null && planoResponse.getListaPeriodicidades().size() > 0 ? planoResponse.getListaPeriodicidades().get(0).getDescricao() : null)
                .periodicidadeNumero(planoResponse.getListaPeriodicidades() != null && planoResponse.getListaPeriodicidades().size() > 0 ? Integer.valueOf(planoResponse.getListaPeriodicidades().get(0).getValor()): null)
                .permiteBalao(planoResponse.getParcelasIntermediarias() != null && BooleanUtils.isTrue(planoResponse.getParcelasIntermediarias().getPermiteBalao()) ? planoResponse.getParcelasIntermediarias().getPermiteBalao() : false)
                .balaoUltima(planoResponse.getParcelasIntermediarias() != null && planoResponse.getParcelasIntermediarias().getBalaoUltima() != null ? planoResponse.getParcelasIntermediarias().getBalaoUltima() : false)
                .controleBalao(getControleBalao(planoResponse.getParcelasIntermediarias()))
                .parcelasIntermediarias(planoResponse.getParcelasIntermediarias() != null ? planoResponse.getParcelasIntermediarias() : null)
                .habilitaGeoLocalizacao(planoResponse.getHabilitaGeoLocalizacao() != null ? planoResponse.getHabilitaGeoLocalizacao() : false)
                .parametrosGeoLocalizacao(planoResponse.getParametrosGeoLocalizacao() != null ? planoResponse.getParametrosGeoLocalizacao() : null)
                .usoPlano(null)  //nao encontrei correspondente
                .build();
    }

    private EnumControleBalao getControleBalao(ParcelasIntermediarias parcelasIntermediarias) {
        if (parcelasIntermediarias != null) {
            //OBRIGATORIO_ULTIMA_PARCELA("OU", "OU"),
            if (BooleanUtils.isTrue(parcelasIntermediarias.getPermiteBalao())
                && BooleanUtils.isTrue(parcelasIntermediarias.getBalaoUltima())
                && BooleanUtils.isTrue(parcelasIntermediarias.getObrigaBalao())){
                return EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA;
            //OBRIGATORIO_BALAO("O", "O"),
            } else if (BooleanUtils.isTrue(parcelasIntermediarias.getPermiteBalao())
                && BooleanUtils.isTrue(parcelasIntermediarias.getObrigaBalao())) {
                return EnumControleBalao.OBRIGATORIO_BALAO;
            //PERMITE_BALAO_ULTIMA_PARCELA("PU", "PU"),
            } else if (BooleanUtils.isTrue(parcelasIntermediarias.getPermiteBalao())
                && BooleanUtils.isTrue(parcelasIntermediarias.getBalaoUltima())) {
                return EnumControleBalao.PERMITE_BALAO_ULTIMA_PARCELA;
            //PERMITE_BALAO("P", "P")
            } else if (BooleanUtils.isTrue(parcelasIntermediarias.getPermiteBalao())) {
                return EnumControleBalao.PERMITE_BALAO;
            } else {
                return EnumControleBalao.NAO_PERMITE_BALAO;
            }
        }

        return EnumControleBalao.NAO_PERMITE_BALAO;
    }

    @Override
    public Plano getPlanoCicloToyota(BigDecimal valorBem, BigDecimal valorEntrada, FiltrosPlano filtrosPlano) {
        List<Plano> planos;

        planos = converteListaDePlanosMotorTaxas(getPlanos(filtrosPlano));

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

    @Override
    public Taxa findTaxas(Integer planoId) {
        String url = apiMotorTaxaUrl + "/plano/" + planoId + "/taxa";
        ParameterizedTypeReference<TaxasResponse> responseType = new ParameterizedTypeReference<TaxasResponse>() {};

        try {
            // esse request não deve passar pelo gateway para que seja o mais rápido possível
            ResponseEntity<TaxasResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
                    null, responseType);
            TaxasResponse taxas = responseEntity.getBody();
            return taxas.getTaxa();
        } catch (HttpClientErrorException.NotFound ex) {
            log.error("não encontrado: " + ex.getResponseBodyAsString());
            List<Erro> l = errorParser.parseList(ex);
            if (!l.isEmpty()) {
                throw new EntityNotFoundException(l.iterator().next());
            } else {
                throw new EntityNotFoundException(Taxa.class, planoId.toString());
            }
        }
    }

    public String findFator(FatorRequest fatorRequest) {
        String url = apiMotorTaxaUrl + "/fator";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("cnpj-origem-negocio", fatorRequest.getCnpjOrigemNegocio())
                .queryParam("rating", fatorRequest.getRating())
                .queryParam("plano-id", fatorRequest.getPlanoId());

        try {
            // esse request não deve passar pelo gateway para que seja o mais rápido possível
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    builder.toUriString(), HttpMethod.GET, new HttpEntity<>(fatorRequest), String.class);
            return responseEntity.getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            log.error("Não encontrado: " + ex.getResponseBodyAsString());
            List<Erro> l = errorParser.parseList(ex);
            if (!l.isEmpty() && l.stream().anyMatch(e -> verificaError(e.getMessage()))) {
            	return "";
            } else {
                throw new EntityNotFoundException(String.class, fatorRequest.getPlanoId().toString());
            }
        }
    }

    private Boolean verificaError(String m) {
    	if(m.equals("Fator não encontrado(a)!"))return true;
    	if(m.equals("Loja não encontrado(a)!"))return true;
    	return false;
    }
//    public Taxa findTaxas(Integer planoId) {
//        PlanoMotorTaxa plano = getPlanoPorId(planoId);
//        br.com.bancotoyota.services.simulador.beans.response.Taxa taxa = plano.getTaxa();
//
//        Taxa taxaToReturn = new Taxa();
//        taxaToReturn.setBaseline(taxa.getBaseline());
//        taxaToReturn.setDataFinalVigencia(taxa.getDataFinalVigencia());
//        taxaToReturn.setDataInicialVigencia(taxa.getDataInicialVigencia());
//        taxaToReturn.setDescricao(taxa.getDescricao());
//        taxaToReturn.setId(taxa.getId());
//        taxaToReturn.setPrazoValidade(taxa.getPrazoValidade());
//        taxaToReturn.setStatusVersionamento(taxa.getStatusVersionamento());
//        taxaToReturn.setVersao(taxa.getVersao());
//
//        List<TaxasDetalhe> taxasFiltradas = new ArrayList<>();
//
//        if (Objects.nonNull(taxa)) {
//            Periodicidade periodicidade = plano.getListaPeriodicidades().get(0);
//
//            int parcelas = Constants.QUTDE_PARCELAS / Integer.parseInt(periodicidade.getValor());
//
//            if (parcelas == Constants.QUTDE_PARCELAS) {
//                taxasFiltradas.addAll(taxa.getTaxasDetalhe());
//            } else {
//                taxasFiltradas.addAll(taxa.getTaxasDetalhe().subList(0, parcelas));
//            }
//        }
//
//        taxaToReturn.setTaxasDetalhe(taxasFiltradas);
//
//        return taxaToReturn;
//    }

    private List<Prazo> convertTaxastoPrazos(Taxa taxas) {
        List<Prazo> listaPrazos = new ArrayList<>();
        taxas.getTaxasDetalhe().forEach(taxasDetalhe -> {
                    for(int i=taxasDetalhe.getPrazoMinimo();i<=taxasDetalhe.getPrazoMaximo();i++) {
                        listaPrazos.add(Prazo.builder()
                                .numeroDeParcelas(i)
                                .taxaBanco(taxasDetalhe.getTaxaMes())
                                .percentualMinEntrada(taxasDetalhe.getPercentualMinimoEntrada())
                                .percentualMaxEntrada(taxasDetalhe.getPercentualMaximoEntrada())
                                .percentualMinBalao(taxasDetalhe.getPercentualMinimoBalao())
                                .percentualMaxBalao(taxasDetalhe.getPercentualMaximoBalao())
                                .build());
                    }
                }
        );

        return listaPrazos;
    }

    @Override
    public Subsidio findSubsidio(Integer planoId) {
        SubsidioMotorTaxa subsidioFromMotorTaxa = this.findSubsidioFromMotorTaxa(planoId);

        // Conversão do dado retornado para o formato já conhecido pelo simulador
        Subsidio subsidio = convertSubsidioFromMotorTaxa(subsidioFromMotorTaxa);

        // BTO-3577-Bugfix: Quando há subsídio, ordena suas faixas de valor por ordem crescente de valor mínimo.
        if (subsidio != null && subsidio.getFaixasValor() != null && !subsidio.getFaixasValor().isEmpty()) {
            List faixasValorOrdenadas = subsidio.getFaixasValor().stream().sorted(
                    Comparator.comparing(Subsidio.FaixaValor::getValorInicial)
            ).collect(Collectors.toList());
            subsidio.setFaixasValor(faixasValorOrdenadas);
        }

        return subsidio;
    }

    @Override
    public SubsidioMotorTaxa findSubsidioFromMotorTaxa(Integer planoId) {
        String url = apiMotorTaxaUrl + "/plano/" + planoId + "/subsidio";
        ParameterizedTypeReference<SubsidioMotorTaxaResponse> responseType = new ParameterizedTypeReference<SubsidioMotorTaxaResponse>() {};

        try {
            // esse request não deve passar pelo gateway para que seja o mais rápido possível
            ResponseEntity<SubsidioMotorTaxaResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
                    null, responseType);
            SubsidioMotorTaxa subsidio = new SubsidioMotorTaxa();
            if (responseEntity.getBody() != null) {
                subsidio = responseEntity.getBody().getSubsidio();
            }
            return subsidio;
        } catch (HttpClientErrorException.NotFound ex) {
            log.error("não encontrado: " + ex.getResponseBodyAsString());
            List<Erro> l = errorParser.parseList(ex);
            if (!l.isEmpty()) {
                throw new EntityNotFoundException(l.iterator().next());
            } else {
                throw new EntityNotFoundException(Subsidio.class, planoId.toString());
            }
        }
    }

    private Subsidio convertSubsidioFromMotorTaxa(SubsidioMotorTaxa subsidioMotorTaxa) {
        if (ObjectUtils.isNotEmpty(subsidioMotorTaxa) && subsidioMotorTaxa.getTipoSubsidio() != null){
            return Subsidio.builder()
                    .tipoSubsidio(EnumTipoSubsidio.from(subsidioMotorTaxa.getTipoSubsidio().getKey()))  // Verificar F ou V
                    .taxaBanco(subsidioMotorTaxa.getTaxaBanco())
                    .fixoPercentualRevendedora(subsidioMotorTaxa.getRevendedora() != null ? subsidioMotorTaxa.getRevendedora().getFixoPercentual() : null)
                    .fixoPercentualMontadora(subsidioMotorTaxa.getMontadora() != null ? subsidioMotorTaxa.getMontadora().getFixoPercentual() : null)
                    .tipoDistribuicao(subsidioMotorTaxa.getTipoDistribuicao() != null ? EnumTipoDistribuicao.from(subsidioMotorTaxa.getTipoDistribuicao().getKey()) : null)  // Verificar P- "Percentual" ou F - "Faixa"
                    .faixasValor(convertSubsidioFaixaValor(subsidioMotorTaxa.getFaixasValorMotorTaxas()))
                    .vpPercentualDistrMarca(subsidioMotorTaxa.getMontadora() != null ? subsidioMotorTaxa.getMontadora().getPercentual() : null)
                    .vpValorMaxMarca(subsidioMotorTaxa.getMontadora() != null ? subsidioMotorTaxa.getMontadora().getValorMaximo() : null)
                    .build();
        }
        return null;
    }

    private List<Subsidio.FaixaValor> convertSubsidioFaixaValor(List<FaixasValorMotorTaxas> faixasValorMotorTaxas) {
        if (faixasValorMotorTaxas != null) {
            List<Subsidio.FaixaValor> listaFaixaValor = new ArrayList<>();
            faixasValorMotorTaxas.forEach(faixaValor -> {
                        listaFaixaValor.add(Subsidio.FaixaValor.builder()
                                .sequencial(faixaValor.getSequencial())
                                .responsabilidade(EnumResponsabilidade.from(faixaValor.getResponsabilidade().getKey())) // Verificar
                                .valorInicial(faixaValor.getValorInicial())
                                .valorFinal(faixaValor.getValorFinal())
                                .build());
                    }
            );
            return listaFaixaValor;
        }
        return null;
    }

    @Override
    public List<PlanoMotorTaxa> getPlanos() {
        log.info("Obtendo os planos de financiamento...");
        try {
            String url = String.format(apiMotorTaxaUrl.concat("/planos/filtros"));
            log.info("Endpoint: {}", url);
            ResponseEntity<PlanosMotorTaxaResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, PlanosMotorTaxaResponse.class);
            List<PlanoMotorTaxa> planosResponse = responseEntity.getBody().getPlanos();
            log.info("Foram encontrados {} planos", planosResponse.size());
            return planosResponse;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ThirdPartyException(ERRO_NA_API_DE_PLANOS_DE_FINANCIAMENTO, e);
        } catch (HttpStatusCodeException e) {
            throw new ThirdPartyException(ERRO_NA_API_DE_PLANOS_DE_FINANCIAMENTO, e);
        }
    }

    @Override
    public List<PlanoMotorTaxa> getPlanos(FiltrosPlano filtrosPlano) {
        log.info("Obtendo os planos de financiamento...");
        ConfiguracaoPlano configuracao = filtrosPlano.getConfiguracao();

        try {
            String perfilUsuario = filtrosPlano.getPerfilUsuario();
            if (perfilUsuario == null) {
                perfilUsuario = perfilConfiguradoUsuario;
            }

            String url = String.format(apiMotorTaxaUrl.concat("/planos/filtros?" +
                            "modalidade=%s&marca-id=%s&tipo-pessoa=%s&zero-km=%s" +
                            "&venda-direta=%s&plano-subsidiado=%s&ano-modelo=%s" +
                            "&codigo-modelo=%s&regiao=%s&perfil=%s&cnpj-origem-negocio=%s"),
                    configuracao.getModalidade(),
                    filtrosPlano.getMarca(),
                    configuracao.getTipoPessoa(),
                    configuracao.getSituacaoVeiculo().equals(SituacaoVeiculo.ZERO_KM),
                    configuracao.isVendaDireta(),
                    configuracao.isSubsidio(),
                    filtrosPlano.getAnoModelo(),
                    filtrosPlano.getModelo(),
                    filtrosPlano.getRegiao(),
                    perfilUsuario,
                    filtrosPlano.getCnpjOrigemNegocio());

            if (!StringUtils.isBlank(filtrosPlano.getAplicacao())) {
                url = String.format(url.concat("&aplicacao=%s"),filtrosPlano.getAplicacao());
            }

            log.info("Endpoint: {}", url);
            log.info("Situacao Veiculo: {}", configuracao.getSituacaoVeiculo());

            ResponseEntity<PlanosMotorTaxaResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, PlanosMotorTaxaResponse.class);

            List<PlanoMotorTaxa> planosResponse = responseEntity.getBody().getPlanos();

            log.info("Foram encontrados {} planos", planosResponse.size());
            return planosResponse;
        } catch (HttpClientErrorException.NotFound e) {
            if (JsonUtils.canConvert(e.getResponseBodyAsString(), new TypeReference<List<MessageHolder>>(){})) {
                log.error(String.format("Não foram encontrados planos de financiamento para a modalidade '%s', " +
                                "tipo pessoa '%s', marca '%s', modelo '%s' '%s' ano '%s', %s venda direta, %s subsídio e " +
                                "origem negócio com CNPJ '%s' na região '%s", configuracao.getModalidade(),
                        configuracao.getTipoPessoa(), filtrosPlano.getMarca(), filtrosPlano.getModelo(),
                        configuracao.getSituacaoVeiculo(), filtrosPlano.getAnoModelo(),
                        configuracao.isVendaDireta() ? "com" : "sem",
                        configuracao.isSubsidio() ? "com" : "sem",
                        filtrosPlano.getCnpjOrigemNegocio(), filtrosPlano.getRegiao()));
                throw new EntityNotFoundException(Plano.class, "plano-financiamento");
            }
            throw new ThirdPartyException(ERRO_NA_API_DE_PLANOS_DE_FINANCIAMENTO, e);
        } catch (HttpStatusCodeException e) {
            throw new ThirdPartyException(ERRO_NA_API_DE_PLANOS_DE_FINANCIAMENTO, e);
        }
    }

    private List<Plano> converteListaDePlanosMotorTaxas(List<PlanoMotorTaxa> planos) {
        return planos
                .stream()
                .map(this::convertPlanoFromMotorTaxa)
                .collect(Collectors.toList());
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
                    .filter(p -> Boolean.TRUE.equals(p.getParcelasIntermediarias().getPermiteBalao()))
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
}
