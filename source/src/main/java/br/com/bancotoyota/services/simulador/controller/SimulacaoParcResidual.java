package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.*;
import br.com.bancotoyota.services.simulador.beans.geolocalizacao.DadosCobrancaServicoGeolocalizacao;
import br.com.bancotoyota.services.simulador.beans.request.ParcelaIntermediaria;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.response.CalculoSeguroMecanicaResponse;
import br.com.bancotoyota.services.simulador.beans.response.Fluxo;
import br.com.bancotoyota.services.simulador.beans.response.SeguroFranquia;
import br.com.bancotoyota.services.simulador.beans.response.TipoDeSimulacao;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.CalculadoraSeguroMecanicaServices;
import br.com.bancotoyota.services.simulador.services.CalculadoraSeguroFranquiaServices;
import br.com.bancotoyota.services.simulador.services.CalculadoraServices;
import br.com.bancotoyota.services.simulador.services.FluxoService;
import br.com.bancotoyota.services.simulador.services.GeolocalizacaoService;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.services.impl.GeolocalizacaoServiceImpl;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import br.com.bancotoyota.simuladorpropostasservices.entidades.simulacao.ResultadoCalculado;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static br.com.bancotoyota.services.simulador.entities.EnumOrigemSolicitacao.*;

public class SimulacaoParcResidual {

    private DadosSimulacaoParcResiduais dados;
    private Map<Integer, DadosSimulacao> dadosSimulacaoMap = new HashMap<>();
    private Map<String, DadosSimulacao> dadosSimulacaoSeguroMap = new HashMap<>();
    private CalculadoraServices calculadoraService;
    @Autowired
    private CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices;
    private Map<Integer, List<ItemFinanciavel>> itensFinanciaveisPorPrazoMap = new HashMap<>();
    private Map<Integer, DadosCobrancaServicoGeolocalizacao> dadosCobrancaGeolocalizacaoPorPrazoMap = new HashMap<>();

    @Getter
    private LimitesSubsidioValor limitesSubsidioValor;
    private static final BigDecimal DEZ_MIL = new BigDecimal(10000);
    private static final BigDecimal UM_CENTESIMO = new BigDecimal("0.01");
    private static FluxoService fluxoService = FluxoService.getInstance();

    private FeatureToggleConfig featureToggleConfig;

    @Autowired
    private CalculadoraSeguroFranquiaServices seguroFranquiaServices;

    @Getter
    @Setter
    private GeolocalizacaoService geolocalizacaoService;

    public SimulacaoParcResidual(FeatureToggleConfig featureToggleConfig, CalculadoraSeguroFranquiaServices seguroFranquiaServices) {
        this.featureToggleConfig = featureToggleConfig;
        this.seguroFranquiaServices = seguroFranquiaServices;

    }

    public SimulacaoParcResidual(CalculadoraServices calculadoraService, LimitesSubsidioValor limitesSubsidioValor, FeatureToggleConfig featureToggleConfig, CalculadoraSeguroFranquiaServices seguroFranquiaServices, CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices) {
        this.calculadoraService = calculadoraService;
        this.limitesSubsidioValor = limitesSubsidioValor;
        this.featureToggleConfig = featureToggleConfig;
        this.seguroFranquiaServices = seguroFranquiaServices;
        this.calculadoraSeguroMecanicaServices = calculadoraSeguroMecanicaServices;
    }

    public void init(SimulacaoParcResidualRequest request, DadosCalculo dadosCalculo, BigDecimal valorFinanciado,
                     List<Prazo> prazos) {
        dados = new DadosSimulacaoParcResiduais(request, dadosCalculo, valorFinanciado, prazos);
        geolocalizacaoService = new GeolocalizacaoServiceImpl(featureToggleConfig);
    }

    public void limparSimulacao(List<Prazo> prazos, TipoDeSeguroPrestamista tipoFixo) {
        dados.setPrazos(prazos);
        dados.getDadosCalculo().setTipoFixo(tipoFixo);
        dadosSimulacaoMap.clear();
        dadosSimulacaoSeguroMap.clear();
    }

    public Seguro getSPF() {
        return dados.getDadosCalculo().getSeguro(TipoDeSeguroPrestamista.SPF);
    }

    /**
     * @param somenteFluxo indica se devemos retornar somente o fluxo ou a resposta
     *                     completa
     * @return
     */
    public List<ResultadoCalculadoParcelaResidual> fazerCalculo(boolean somenteFluxo) {
        List<ResultadoCalculado> resultadosCalculados = fazerCalculo();

        Collection<ResultadoCalculadoParcelaResidual> resultados;
        if (somenteFluxo) {
            resultados = prepararRespostaFluxo(resultadosCalculados);
        } else {
            resultados = prepararResposta(resultadosCalculados);
        }

        // retorna a lista ordenada pelo número de parcelas
        List<ResultadoCalculadoParcelaResidual> listaOrdenada = new ArrayList<>(resultados);
        listaOrdenada.sort(Comparator.comparingInt(ResultadoCalculadoParcelaResidual::getPrazo));


        return listaOrdenada;
    }

    public List<ResultadoCalculadoParcelaResidual> fazerCalculoTaxas(boolean somenteFluxo) {
        List<ResultadoCalculado> resultadosCalculados = fazerCalculoInicialTaxas();

        Collection<ResultadoCalculadoParcelaResidual> resultados;
        if (somenteFluxo) {
            resultados = prepararRespostaFluxo(resultadosCalculados);
        } else {
            resultados = prepararResposta(resultadosCalculados);
        }

        // retorna a lista ordenada pelo número de parcelas
        List<ResultadoCalculadoParcelaResidual> listaOrdenada = new ArrayList<>(resultados);
        listaOrdenada.sort(Comparator.comparingInt(ResultadoCalculadoParcelaResidual::getPrazo));


        return listaOrdenada;
    }

    private List<ResultadoCalculado> fazerCalculo() {
        // Faz um cálculo inicial sem considerar o valor da parcela para determinar o
        // tipo de seguro prestamista
        List<ResultadoCalculado> resultadosCalculados = fazerCalculoInicial();

        // verificar se o valor da parcela passou do limite configurado para o seguro
        // prestamista e troca ou remove o
        // seguro conforme o caso (a troca é feita no objeto DadosSimulacao, mas o
        // re-cálculo só é feito a seguir)
        Map<Integer, DadosSimulacao> novasSimulacoes = verificaValorDaParcela(resultadosCalculados);

        if (!novasSimulacoes.isEmpty()) {
            // refaz as simulações onde houve troca de seguro prestamista
            resultadosCalculados.addAll(calculadoraService.calcular(novasSimulacoes.values()));
        }

        return resultadosCalculados;
    }

    public List<BigDecimal> fazerCalculoSimples() {
        // Faz um cálculo inicial sem considerar o valor da parcela para determinar o
        // tipo de seguro prestamista
        List<ResultadoCalculado> resultadosCalculados = fazerCalculo();
        return resultadosCalculados.stream().map(rc -> fluxoService.getValorDaParcela(rc)).collect(Collectors.toList());
    }

    private void calcularValoresDosSegurosNaParcela(List<ResultadoCalculado> resultadosCalculados) {
        Map<Integer, ResultadoCalculado> resultadoMap = resultadosCalculados.stream().collect(
                Collectors.toMap(rc -> Integer.parseInt(rc.getDadosSimulacao().getPrazo()), Function.identity()));

        dados.getPrazos().forEach(prazo -> {
            ResultadoCalculado resultadoCalculado = resultadoMap.get(prazo.getNumeroDeParcelas());

            // Quando o subsídio é por valor precisamos calcular a taxa correspondente na
            // simulação do valor do bem e depois
            // aplicar essa taxa quando vamos calcular o valor dos seguros na parcela.
            BigDecimal taxaDeSubsidio = dados.getRequest().getValorSubsidio() != null
                    ? new BigDecimal(resultadoCalculado.getDadosSimulacao().getTaxaOperacao()).multiply(NumberUtils.CEM)
                    : null;

            boolean simularSeguroAuto = dados.getRequest().getValorSeguroAuto().compareTo(BigDecimal.ZERO) != 0;
            BigDecimal seguroPrestamista = new BigDecimal(resultadoCalculado.getDadosSimulacao().getValorSeguro());
            boolean simularSeguroPrestamista = seguroPrestamista.compareTo(BigDecimal.ZERO) != 0;


            List<ItemFinanciavel> list = dados.getRequest().getItens();
            if (Objects.nonNull(prazo.getDadosCobrancaServicoGeolocalizacao())
                    && Objects.nonNull(prazo.getDadosCobrancaServicoGeolocalizacao().getItemFinanciavelServicoGeolocalizacao())) {
                list.add(prazo.getDadosCobrancaServicoGeolocalizacao().getItemFinanciavelServicoGeolocalizacao());
            }

            int numeroDeItens = list == null ? 0 : list.size();
            if (simularSeguroAuto) {
                numeroDeItens++;
            }
            if (simularSeguroPrestamista) {
                numeroDeItens++;
            }

            if (numeroDeItens != 0) {
                prepararSimulacaoParaValorNaParcela(prazo, taxaDeSubsidio);
            }
        });

        resultadosCalculados.addAll(calculadoraService.calcular(dadosSimulacaoSeguroMap.values()));
    }

    /**
     * Faz o cálculo das simulações já com seguro prestamista, mas sem levar em
     * conta o valor da parcela. Posteriormente, já com o valor da parcela calculado
     * aqui, vamos verificar se precisa mudar o seguro prestamista.
     */
    private List<ResultadoCalculado> fazerCalculoInicial() {
        if (dados.getPrazos().get(0).getNumeroDeParcelas().equals(24)) {
            String a = "";
        }

        // Calcula o custoPercentualDoSeguroPrestamista e o valorDoSeguroPrestamista considerando o valorFinanciado
        // que veio do método criarSimulacao, sem considerar variação de prazos (como é o caso da geolocalização).
        CustoPercentualDoSeguroPrestamista custoPercentualDoSeguroPrestamista = CustoPercentualDoSeguroPrestamista
                .getFatorDoSeguro(dados.getDadosCalculo(), dados.getRequest().getParametrosDeSeguroPrestamista(),
                        dados.getValorFinanciado(), null, dados.getRequest().getIdade(),
                        dados.getRequest().getTipoPessoaEnum());

        BigDecimal valorDoSeguroFranquia = dados.getRequest().getVlrSeguroFranquia() == null ? BigDecimal.ZERO : dados.getRequest().getVlrSeguroFranquia();
        dados.getRequest().setVlrSeguroFranquia(valorDoSeguroFranquia);

        // Para cada prazo, prepara a simulação do bem, podendo ou não colocar o serviço de geolocalização na conta
        dados.getPrazos().forEach(prazo -> {
            BigDecimal novoValorFinanciado = dados.getValorFinanciado();

            // Inicialmente salve todos os itens financiaveis, se houver, da request nessa variável, que será usada mais adiante.
            List<ItemFinanciavel> itensFinanciaveis = this.obterItensFinanciaveis(dados.getRequest().getItens());

            CalculoSeguroMecanicaResponse seguroGarantiaMecanica = obterSeguroGarantiaMecanica(prazo);
            BigDecimal valorSeguroGarantiaMecanica = obterValorSeguroGarantiaMecanica(seguroGarantiaMecanica);
            if (valorSeguroGarantiaMecanica.compareTo(BigDecimal.ZERO) > 0) {
                novoValorFinanciado = novoValorFinanciado.add(valorSeguroGarantiaMecanica);
            }

            BigDecimal valorDoSeguroPrestamista = novoValorFinanciado
                    .multiply(custoPercentualDoSeguroPrestamista.getFator());

            // Busca dados da geolocalizacao, se houver
            if (dados.getDadosCalculo().getPlano() != null &&
                    dados.getDadosCalculo().getPlano().getHabilitaGeoLocalizacao() != null &&
                    dados.getDadosCalculo().getPlano().getHabilitaGeoLocalizacao() &&
                    dados.getDadosCalculo().getPlano().getParametrosGeoLocalizacao() != null &&
                    geolocalizacaoService != null) {
                DadosCobrancaServicoGeolocalizacao dadosCobrancaServicoGeolocalizacao = geolocalizacaoService.obterDadosCobrancaServicoGeolocalizacao(dados.getRequest(),dados.getDadosCalculo().getPlano(), prazo.getNumeroDeParcelas());

                if (Objects.nonNull(dadosCobrancaServicoGeolocalizacao)) {
                    // coloca os dados da cobrança da geolocalizaçao no prazo, para posteriormente voltar na request
                    prazo.setDadosCobrancaServicoGeolocalizacao(dadosCobrancaServicoGeolocalizacao);

                    // Havendo um item financiável de geolocalização, é necessário recalcular o valorFinanciado,
                    // e, por conseguinte, o custo do percentual do seguro prestamista e o valor do seguro prestamista
                    // que dependem dessem valorFinanciado.

                    if (Objects.nonNull(dadosCobrancaServicoGeolocalizacao.getItemFinanciavelServicoGeolocalizacao())) {
                        itensFinanciaveis.add(dadosCobrancaServicoGeolocalizacao.getItemFinanciavelServicoGeolocalizacao());

                        novoValorFinanciado = dados.getValorFinanciado().add(dadosCobrancaServicoGeolocalizacao.getItemFinanciavelServicoGeolocalizacao().getValor());
                    }

                    CustoPercentualDoSeguroPrestamista novoCustoPercentualDoSeguroPrestamista = CustoPercentualDoSeguroPrestamista
                            .getFatorDoSeguro(dados.getDadosCalculo(), dados.getRequest().getParametrosDeSeguroPrestamista(),
                                    novoValorFinanciado, null, dados.getRequest().getIdade(),
                                    dados.getRequest().getTipoPessoaEnum());

                    BigDecimal novoValorDoSeguroPrestamista = novoValorFinanciado
                            .multiply(novoCustoPercentualDoSeguroPrestamista.getFator());

                    dadosCobrancaGeolocalizacaoPorPrazoMap.put(prazo.getNumeroDeParcelas(), dadosCobrancaServicoGeolocalizacao);
                    itensFinanciaveisPorPrazoMap.put(prazo.getNumeroDeParcelas(), itensFinanciaveis);

                    prepararSimulacaoDoValorDoBem(prazo, novoCustoPercentualDoSeguroPrestamista, novoValorDoSeguroPrestamista, itensFinanciaveis, seguroGarantiaMecanica);
                } else {
                    // Se não houver geolocalização, adiciona o custoPercentualDoSeuroPrestaMista e valorDoSeguroPrestamista
                    // calculados no iníocio do metodo SEM CONSIDERAR geolocalização.
                    prepararSimulacaoDoValorDoBem(prazo, custoPercentualDoSeguroPrestamista, valorDoSeguroPrestamista,itensFinanciaveis, seguroGarantiaMecanica);
                }
            } else {
                // Se não houver geolocalização, adiciona o custoPercentualDoSeuroPrestaMista e valorDoSeguroPrestamista
                // calculados no iníocio do metodo SEM CONSIDERAR geolocalização.
                prepararSimulacaoDoValorDoBem(prazo, custoPercentualDoSeguroPrestamista, valorDoSeguroPrestamista, itensFinanciaveis, seguroGarantiaMecanica);
            }
        });

        return calculadoraService.calcular(dadosSimulacaoMap.values());
    }

    private List<ResultadoCalculado> fazerCalculoInicialTaxas() {
        BigDecimal valorDoSeguroPrestamista = dados.getDadosCalculo().getValorSeguroPrestamista() != null ? dados.getDadosCalculo().getValorSeguroPrestamista() : BigDecimal.ZERO;
        BigDecimal valorDoSeguroFranquia = dados.getRequest().getVlrSeguroFranquia() == null ? BigDecimal.ZERO : dados.getRequest().getVlrSeguroFranquia();
        dados.getRequest().setVlrSeguroFranquia(valorDoSeguroFranquia);

        // Para cada prazo, prepara a simulação do bem, podendo ou não colocar o serviço de geolocalização na conta
        dados.getPrazos().forEach(prazo -> {
            BigDecimal valorFinanciado = dados.getValorFinanciado();
            // Inicialmente salve todos os itens financiaveis, se houver, da request nessa variável, que será usada mais adiante.
            List<ItemFinanciavel> itensFinanciaveis = this.obterItensFinanciaveis(dados.getRequest().getItens());

            CalculoSeguroMecanicaResponse seguroGarantiaMecanica = obterSeguroGarantiaMecanica(prazo);
            BigDecimal valorSeguroGarantiaMecanica = obterValorSeguroGarantiaMecanica(seguroGarantiaMecanica);
            if (valorSeguroGarantiaMecanica.compareTo(BigDecimal.ZERO) > 0) {
                valorFinanciado = valorFinanciado.add(valorSeguroGarantiaMecanica);
            }

            // Busca dados da geolocalizacao, se houver
            if (geolocalizacaoService != null && dados.getDadosCalculo().getPlano() != null) {
                DadosCobrancaServicoGeolocalizacao dadosCobrancaServicoGeolocalizacao = geolocalizacaoService.obterDadosCobrancaServicoGeolocalizacao(dados.getRequest(),dados.getDadosCalculo().getPlano(), prazo.getNumeroDeParcelas());

                if (Objects.nonNull(dadosCobrancaServicoGeolocalizacao)) {
                    // coloca os dados da cobrança da geolocalizaçao no prazo, para posteriormente voltar na request
                    prazo.setDadosCobrancaServicoGeolocalizacao(dadosCobrancaServicoGeolocalizacao);

                    // Havendo um item financiável de geolocalização, é necessário recalcular o valorFinanciado,
                    // e, por conseguinte, o custo do percentual do seguro prestamista e o valor do seguro prestamista
                    // que dependem dessem valorFinanciado.
                    BigDecimal novoValorFinanciado = valorFinanciado;
                    if (Objects.nonNull(dadosCobrancaServicoGeolocalizacao.getItemFinanciavelServicoGeolocalizacao())) {
                        itensFinanciaveis.add(dadosCobrancaServicoGeolocalizacao.getItemFinanciavelServicoGeolocalizacao());

                        novoValorFinanciado = valorFinanciado.add(dadosCobrancaServicoGeolocalizacao.getItemFinanciavelServicoGeolocalizacao().getValor());
                    }

                    CustoPercentualDoSeguroPrestamista novoCustoPercentualDoSeguroPrestamista = CustoPercentualDoSeguroPrestamista
                            .getFatorDoSeguro(dados.getDadosCalculo(), dados.getRequest().getParametrosDeSeguroPrestamista(),
                                    novoValorFinanciado, null, dados.getRequest().getIdade(),
                                    dados.getRequest().getTipoPessoaEnum());

                    BigDecimal novoValorDoSeguroPrestamista = novoValorFinanciado
                            .multiply(novoCustoPercentualDoSeguroPrestamista.getFator());

                    dadosCobrancaGeolocalizacaoPorPrazoMap.put(prazo.getNumeroDeParcelas(), dadosCobrancaServicoGeolocalizacao);
                    itensFinanciaveisPorPrazoMap.put(prazo.getNumeroDeParcelas(), itensFinanciaveis);

                    prepararSimulacaoDoValorDoBem(prazo, novoCustoPercentualDoSeguroPrestamista, novoValorDoSeguroPrestamista, itensFinanciaveis, seguroGarantiaMecanica);
                } else {
                    // Se não houver geolocalização, adiciona o custoPercentualDoSeuroPrestaMista e valorDoSeguroPrestamista
                    // calculados no iníocio do metodo SEM CONSIDERAR geolocalização.
                    prepararSimulacaoDoValorDoBem(prazo, null, valorDoSeguroPrestamista, itensFinanciaveis, seguroGarantiaMecanica);
                }
            } else {
                // Se não houver geolocalização, adiciona o custoPercentualDoSeuroPrestaMista e valorDoSeguroPrestamista
                // calculados no iníocio do metodo SEM CONSIDERAR geolocalização.
                prepararSimulacaoDoValorDoBem(prazo, null, valorDoSeguroPrestamista, itensFinanciaveis, seguroGarantiaMecanica);
            }
        });

        return calculadoraService.calcular(dadosSimulacaoMap.values());
    }

    private List<ResultadoCalculadoParcelaResidual> prepararRespostaFluxo(
            List<ResultadoCalculado> resultadosCalculados) {
        return resultadosCalculados.stream().filter(resultadoCalculado -> resultadoCalculado.getDadosSimulacao()
                .getIdSimulacao().equals(TipoDeSimulacao.RESIDUAL.toString())).map(resultadoCalculado -> {
            int prazo = Integer.parseInt(resultadoCalculado.getDadosSimulacao().getPrazo());
            ResultadoCalculadoParcelaResidual resultado = new ResultadoCalculadoParcelaResidual();
            resultado.setPrazo(prazo);
            resultado.setFluxo(fluxoService.getFluxo(resultadoCalculado, dados.getRequest().getValorEntrada(),
                    dados.getDadosCalculo().getDataCalculo()));
            return resultado;
        }).collect(Collectors.toList());
    }

    private Collection<ResultadoCalculadoParcelaResidual> prepararResposta(
            List<ResultadoCalculado> resultadosCalculados) {

        if (!resultadosCalculados.isEmpty() && dados.getRequest().isRetornarValorNaParcela()) {
            calcularValoresDosSegurosNaParcela(resultadosCalculados);
        }
        // Vamos incluir o fluxo na resposta quando for passado apenas um prazo, desta
        // forma evitamos ter que fazer
        // uma segunda chamada só para pegar o fluxo.
        boolean incluirFluxo = dados.getRequest().isRetornarFluxoPrazoUnico()
                && resultadosCalculados.stream().filter(resultadoCalculado -> resultadoCalculado.getDadosSimulacao()
                .getIdSimulacao().equals(TipoDeSimulacao.RESIDUAL.toString())).count() == 1;
        Map<Integer, ResultadoCalculadoParcelaResidual> resultados = resultadosCalculados.stream()
                .filter(resultadoCalculado -> resultadoCalculado.getDadosSimulacao().getIdSimulacao()
                        .equals(TipoDeSimulacao.RESIDUAL.toString()))
                .map(r -> criarResultadoCalculadoParcelaResidual(r, incluirFluxo))
                .collect(Collectors.toMap(ResultadoCalculadoParcelaResidual::getPrazo, r -> r));

        resultadosCalculados.stream().filter(resultadoCalculado -> resultadoCalculado.getDadosSimulacao()
                        .getIdSimulacao().equals(TipoDeSimulacao.FATOR_PARA_VALOR_NA_PARCELA.toString()))
                .forEach(resultadoCalculado -> {
                    Integer prazo = Integer.valueOf(resultadoCalculado.getDadosSimulacao().getPrazo());
                    ResultadoCalculadoParcelaResidual resultadoCalculadoParcelaResidual = resultados.get(prazo);
                    BigDecimal fator = fluxoService.getValorDaParcela(resultadoCalculado).divide(DEZ_MIL);
                    resultadoCalculadoParcelaResidual
                            .setValorSeguroAutoNaParcela(dados.getRequest().getValorSeguroAuto().multiply(fator));
                    DadosSimulacao dadosSimulacao = dadosSimulacaoMap.get(prazo);
                    resultadoCalculadoParcelaResidual.setValorDoSeguroPrestamistaNaParcela(
                            dadosSimulacao.getValorSeguroPrestamista().multiply(fator));

                    SeguroFranquia seguroFranquia = seguroFranquiaServices.calcular(fator, dadosSimulacao.getVlrSeguroFranquia());

                    resultadoCalculadoParcelaResidual.setSeguroFranquia(seguroFranquia);
                    List<ItemFinanciavel> list = this.obterItensFinanciaveis(dados.getRequest().getItens());
                    if (Objects.nonNull(itensFinanciaveisPorPrazoMap.get(prazo)) && !itensFinanciaveisPorPrazoMap.get(prazo).isEmpty()) {
                        list = itensFinanciaveisPorPrazoMap.get(prazo);
                    }

            if (list != null && !list.isEmpty()) {
                List<ItemFinanciavel> itensFinanciaveisNaParcela = new ArrayList<>();
                list.forEach(item -> {
                    ItemFinanciavel i = new ItemFinanciavel();
                    i.setCodigo(item.getCodigo());
                    i.setDescricao(item.getDescricao());
                    i.setValor(item.getValor());
                    i.setValorParcela(item.getValor().multiply(fator).setScale(2, RoundingMode.HALF_UP));
                    i.setEditavel(item.isEditavel());
                    i.setRemovivel(item.isRemovivel());
                    itensFinanciaveisNaParcela.add(i);
                });
                resultadoCalculadoParcelaResidual.setItensFinanciaveisNaParcela(itensFinanciaveisNaParcela);
            }
        });

        return resultados.values();
    }

    private ResultadoCalculadoParcelaResidual criarResultadoCalculadoParcelaResidual(
            ResultadoCalculado resultadoCalculado, boolean incluirFluxo) {
        Integer prazo = Integer.valueOf(resultadoCalculado.getDadosSimulacao().getPrazo());
        DadosSimulacao dadosSimulacao = dadosSimulacaoMap.get(prazo);
        BigDecimal valorResidual;
        BigDecimal porcentualParcelaResidual = null;
        if (dados.getRequest().getValorParcelaResidual() != null && (dados.getRequest().getPermiteBalao() == null
                || (dados.getRequest().getPermiteBalao() != null && dados.getRequest().getPermiteBalao()))) {

            valorResidual = new BigDecimal(
                    resultadoCalculado.getDadosSimulacao().getParcelasBalao().get(0).getValorParcela());
            porcentualParcelaResidual = valorResidual.multiply(NumberUtils.CEM).divide(dados.getRequest().getValorBem(),
                    2, RoundingMode.HALF_UP);
            // ciclo-toyota
            if (featureToggleConfig.getBuscaApiMotorTaxaEnabled() && featureToggleConfig.getCicloIntermediariasEnabled() && dados.getRequest().getControleBalao() == EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA) {
                valorResidual = dados.getRequest().getValorParcelaResidual();
            }

        } else {
            // para planos CDC
            valorResidual = null;
        }

        BigDecimal valorParcela = fluxoService.getValorDaParcela(resultadoCalculado);
        BigDecimal valorIOF = NumberUtils.toBigDecimal(resultadoCalculado.getValorIOF());
        BigDecimal valorCetAnual = NumberUtils.toBigDecimal(resultadoCalculado.getValorTaxaCetAnual());
        BigDecimal valorTotalFinanciado = NumberUtils.toBigDecimal(resultadoCalculado.getValorFinanciamento());
        BigDecimal valorTaxaMes = NumberUtils.toBigDecimal(resultadoCalculado.getValorTaxaMes());
        BigDecimal valorTaxaAnual = NumberUtils.toBigDecimal(resultadoCalculado.getValorTaxaAnual());
        BigDecimal valorSubsidio = NumberUtils.toBigDecimal(resultadoCalculado.getValorSubsidio());
        BigDecimal valorTotalFinanciamento = NumberUtils.toBigDecimal(resultadoCalculado.getValorTotalFinanciamento());
        SeguroFranquia seguroFranquia = new SeguroFranquia();
        seguroFranquia.setValorTotal(dadosSimulacao.getVlrSeguroFranquia());
        seguroFranquia.setValorParcela(BigDecimal.ZERO);
        //TODO: ADICIONAR CNPJ SEGURADORA
        //seguroFranquia.setCnpjSeguradora(null);
        BigDecimal taxaBancoMotorTaxas = resultadoCalculado.getValorTaxaBanco();

        List<Fluxo> fluxo = incluirFluxo ? fluxoService.getFluxo(resultadoCalculado,
                dados.getRequest().getValorEntrada(), dados.getDadosCalculo().getDataCalculo()) : null;

        // Coloca os dados da cobrança da geolocalização para esse prazo no resultado calculado da parcela residual
        DadosCobrancaServicoGeolocalizacao dadosCobrancaGeolocalizacaoPorPrazo = dadosCobrancaGeolocalizacaoPorPrazoMap != null ? (dadosCobrancaGeolocalizacaoPorPrazoMap.get(prazo) != null ? dadosCobrancaGeolocalizacaoPorPrazoMap.get(prazo) : null) : null;
        return new ResultadoCalculadoParcelaResidual(prazo, valorParcela, valorIOF, valorCetAnual, valorTotalFinanciado,
                valorTaxaMes, valorTaxaAnual, valorSubsidio, dadosSimulacao.getDadosSeguroMecanica(), dadosSimulacao.getCustoPercentualDoSeguroPrestamista(),
                dadosSimulacao.getValorSeguroPrestamista(), BigDecimal.ZERO, seguroFranquia, valorResidual, NumberUtils.ZERO_2_CASAS,
                null, fluxo, resultadoCalculado.getValorSubsidioUsado(), resultadoCalculado.getTaxaSubsidioUsada(),
                valorTotalFinanciamento, porcentualParcelaResidual, resultadoCalculado,
                valorTaxaMes, valorTaxaAnual, resultadoCalculado.getTaxaSubsidioUsada(), taxaBancoMotorTaxas, dadosCobrancaGeolocalizacaoPorPrazo);
    }

    /**
     * Pega o valor da parcela calculado no passo anterior e verifica se isso causa
     * alguma mudança no seguro prestamista. Se for o caso retorna no mapa para
     * indicar que precisa ser recalculado.
     */
    private Map<Integer, DadosSimulacao> verificaValorDaParcela(List<ResultadoCalculado> resultadosCalculados) {

        Map<Integer, DadosSimulacao> novasSimulacoes = new HashMap<>();
        Iterator<ResultadoCalculado> iterator = resultadosCalculados.iterator();
        while (iterator.hasNext()) {
            ResultadoCalculado resultadoCalculadoEx = iterator.next();
            Integer prazo = Integer.valueOf(resultadoCalculadoEx.getDadosSimulacao().getPrazo());
            DadosSimulacao dadosSimulacao = dadosSimulacaoMap.get(prazo);

            BigDecimal valorParcela = fluxoService.getValorDaParcela(resultadoCalculadoEx).setScale(2,
                    RoundingMode.HALF_UP);

            CustoPercentualDoSeguroPrestamista custoPercentualDoSeguroPrestamista = CustoPercentualDoSeguroPrestamista
                    .getFatorDoSeguro(dados.getDadosCalculo(), dados.getRequest().getParametrosDeSeguroPrestamista(),
                            NumberUtils.toBigDecimal(resultadoCalculadoEx.getValorFinanciamento()), valorParcela, dados.getRequest().getIdade(),
                            dados.getRequest().getTipoPessoaEnum());

            if (custoPercentualDoSeguroPrestamista.getTipo() != dadosSimulacao.getCustoPercentualDoSeguroPrestamista()
                    .getTipo()) {
                // houve mudança do fator do seguro, então precisamos recalcular para esse prazo
                dadosSimulacao.setCustoPercentualDoSeguroPrestamista(custoPercentualDoSeguroPrestamista);

                Integer prazoAnalisado = resultadoCalculadoEx.getDadosSimulacao().getPrazo() != null ? Integer.valueOf(resultadoCalculadoEx.getDadosSimulacao().getPrazo()) : 0;
                if (dadosCobrancaGeolocalizacaoPorPrazoMap != null && dadosCobrancaGeolocalizacaoPorPrazoMap.get(prazoAnalisado) != null) {
                    BigDecimal valorServicoConectado = Objects.nonNull(dadosCobrancaGeolocalizacaoPorPrazoMap.get(prazoAnalisado).getItemFinanciavelServicoGeolocalizacao())
                            ? dadosCobrancaGeolocalizacaoPorPrazoMap.get(prazoAnalisado).getItemFinanciavelServicoGeolocalizacao().getValor()
                            : BigDecimal.ZERO;
                    BigDecimal novoValorFinanciado = dados.getValorFinanciado().add(valorServicoConectado);

                    dadosSimulacao.setValorSeguroPrestamista(
                            novoValorFinanciado.multiply(custoPercentualDoSeguroPrestamista.getFator()));
                } else { // Caminho normal caso não haja geolocalização
                    dadosSimulacao.setValorSeguroPrestamista(
                            dados.getValorFinanciado().multiply(custoPercentualDoSeguroPrestamista.getFator()));
                }

                novasSimulacoes.put(prazo, dadosSimulacao);


                iterator.remove();
            }
        }
        return novasSimulacoes;
    }

    private List<ParcelaBalao> getParcelasBalao(Prazo prazo) {
        List<ParcelaBalao> parcelasBalao = new ArrayList<>();

        SimulacaoParcResidualRequest request = dados.getRequest();

        BigDecimal residualMaximo = obterValorResidualMaximo(request.getValorBem(), prazo.getPercentualMaxBalao());
        BigDecimal residualMinimo = obterValorResidualMinimo(request.getValorBem(), prazo.getPercentualMinBalao(), request.getPermiteBalao());

        List<ParcelaIntermediaria> intermediarias = request.getIntermediarias();

        /*
         * if (intermediarias != null && request.getValorParcelaResidual() != null) {
         * throw new
         * BusinessValidationException("campos valor-parcela-residual e parcelas-intermediarias não pode "
         * + "ser passados ao mesmo tempo"); }
         */

        if (request.getModalidade() == Modalidade.CDC || isMontaIntermediariaQuandoForCiclo(request)) {
            montarIntermediarias(prazo, parcelasBalao, intermediarias);
        } else {
            montarResidual(prazo, parcelasBalao, residualMaximo, residualMinimo);
        }
        return parcelasBalao;
    }

    public BigDecimal obterValorResidualMinimo(BigDecimal valorBem, BigDecimal percentualMinimoBalao, Boolean permiteBalao) {
        BigDecimal residualMinimo = valorBem.multiply(percentualMinimoBalao.divide(NumberUtils.CEM));
        if (permiteBalao != null && permiteBalao) {
            residualMinimo = new BigDecimal(0);
        } else if (residualMinimo.setScale(2, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO) == 0) {
            // AutBank não aceita residual zero para planos com balão
            residualMinimo = UM_CENTESIMO;
        }

        return residualMinimo;
    }

    public BigDecimal obterValorResidualMaximo(BigDecimal valorBem, BigDecimal percentualMaximoBalao) {
        return valorBem.multiply(percentualMaximoBalao.divide(NumberUtils.CEM));
    }

    public boolean isMontaIntermediariaQuandoForCiclo(SimulacaoParcResidualRequest request) {
        List<String> listaAplicacoes = new ArrayList<>(Arrays.asList(DIRECT.getValue(), GERADOR_OFERTA.getValue(), FANDI.getValue()));
        if (request.getOrigemSolicitacao() == null || !listaAplicacoes.contains(request.getOrigemSolicitacao())) {
            return false; // Só existe ciclo com intermediárias no contexto do DIRECT.
        }

        if (request.getValorParcelaDesejada() != null && request.getValorParcelaDesejada().compareTo(BigDecimal.ZERO) > 0) {
            return false;
        }

        // Se não permite balão, não há que se falar em ciclo com intermediárias.
        if (request.getPermiteBalao() == null) {
            return false;
        }

        if (featureToggleConfig == null) {
            return false;
        }

        if (featureToggleConfig.getBuscaApiMotorTaxaEnabled() && featureToggleConfig.getCicloIntermediariasEnabled()) {
            if (request.getModalidade() == Modalidade.CICLO_TOYOTA) {
                return request.getPermiteBalao();
            }
        }

        return false;
    }

    private void montarIntermediarias(Prazo prazo, List<ParcelaBalao> parcelasBalao,
                                      List<ParcelaIntermediaria> intermediarias) {

        if (intermediarias != null) {
            YearMonth primeiroVencimento = YearMonth
                    .from(dados.getRequest().getDataPrimeiroVencimento(dados.getDadosCalculo().getDataCalculo()));
            YearMonth ultimoVencimento = YearMonth.from(primeiroVencimento
                    .plusMonths((long) prazo.getNumeroDeParcelas() * dados.getRequest().getPeriodicidade()));
            for (ParcelaIntermediaria parcelaIntermediaria : intermediarias) {
                if (parcelaIntermediaria.getVencimento() == null) {
                    throw new BusinessValidationException(
                            "Planos com intermediárias o vencimento é obrigatório");
                }
                YearMonth vencimento = YearMonth.from(parcelaIntermediaria.getVencimento());
                if (vencimento.isBefore(primeiroVencimento) || vencimento.isAfter(ultimoVencimento)) {
                    throw new BusinessValidationException("Data " + vencimento
                            + " está fora do intervalo de validade do financiamento: " + primeiroVencimento + " até "
                            + ultimoVencimento + " para prazo de " + prazo.getNumeroDeParcelas() + " parcelas");
                }
                ParcelaBalao parcelaBalao = new ParcelaBalao(parcelaIntermediaria.getVencimento(),
                        parcelaIntermediaria.getValor());
                parcelasBalao.add(parcelaBalao);
            }
        }
    }

    public void montarResidualDoRequest(SimulacaoParcResidualRequest request, Prazo prazo, List<ParcelaBalao> parcelasBalao, BigDecimal residualMaximo,
                                        BigDecimal residualMinimo, LocalDate dataCalculo, boolean aplicarValidacaoMotorTaxa) {
        BigDecimal residual = new BigDecimal(0);
        boolean permiteResidual = (request.getPermiteBalao() == null)
                || (request.getPermiteBalao() != null && request.getPermiteBalao());

        if(aplicarValidacaoMotorTaxa && isMontaIntermediariaQuandoForCiclo(request)){
            permiteResidual = request.getControleBalao() != EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA;
        }

        if (permiteResidual) {
            if (request.getValorParcelaResidual() != null) {
                residual = request.getValorParcelaResidual().setScale(2, RoundingMode.HALF_UP);
            }

            if (residual.compareTo(residualMaximo) >= 0) {
                residual = pegarMaximoArredondando(residualMaximo);
            }

            if (residual.compareTo(residualMinimo) <= 0) {
                residual = pegarMinimoArredondando(residualMinimo);
            }

            ParcelaBalao parcelaBalao = new ParcelaBalao(
                    request.getDataPrimeiroVencimento(dataCalculo)
                            .plusMonths(prazo.getNumeroDeParcelas() - 1L),
                    residual);
            parcelasBalao.add(parcelaBalao);
        }
    }

    private void montarResidual(Prazo prazo, List<ParcelaBalao> parcelasBalao, BigDecimal residualMaximo,
                                BigDecimal residualMinimo) {
        montarResidualDoRequest(dados.getRequest(), prazo, parcelasBalao, residualMaximo, residualMinimo, dados.getDadosCalculo().getDataCalculo(), true);
    }

    public static BigDecimal pegarMinimoArredondando(BigDecimal residualMinimo) {
        BigDecimal residual = residualMinimo;
        BigDecimal arredondado = residual.setScale(2, RoundingMode.HALF_UP);
        if (arredondado.compareTo(residualMinimo) < 0) {
            residual = arredondado.add(UM_CENTESIMO);
        }
        return residual;
    }

    public static BigDecimal pegarMaximoArredondando(BigDecimal residualMaximo) {
        BigDecimal residual = residualMaximo;
        BigDecimal arredondado = residual.setScale(2, RoundingMode.HALF_UP);
        if (arredondado.compareTo(residualMaximo) > 0) {
            residual = arredondado.subtract(UM_CENTESIMO);
        }
        return residual;
    }

    /**
     * @param taxaDeSubsidio só deve ser diferente de null para o caso de subsídio
     *                       por valor e nesse caso vai ser a taxa calculada na
     *                       primeira simulação do valor do bem
     */
    private void prepararSimulacaoParaValorNaParcela(Prazo prazo, BigDecimal taxaDeSubsidio) {
        SimulacaoParcResidualRequest request = dados.getRequest();
        IOF iof = dados.getDadosCalculo().getTaxasIOF();
        if (taxaDeSubsidio == null) {
            taxaDeSubsidio = request.getTaxaSubsidio();
        }

        DadosSimulacao dadosSimulacaoSeguro = new DadosSimulacao(TipoDeSimulacao.FATOR_PARA_VALOR_NA_PARCELA,
                dados.getDadosCalculo().getDataCalculo(),
                request.getDataPrimeiroVencimento(dados.getDadosCalculo().getDataCalculo()), DEZ_MIL, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, taxaDeSubsidio,
                request.getRetorno(), request.getPeriodicidade(), TipoFinanciamento.CDC, TipoCalculo.TAXA, null, false,
                iof.getValorIOFAA(), iof.getValorIOFAD(), iof.getValorBaseCalculoParaIOF(), Collections.emptyList(),
                prazo, request.getTipoPessoa(), null, !request.getNDiasAposVencimento(),
                prazo.getTaxaBanco());
        dadosSimulacaoSeguroMap.put(prazo.getNumeroDeParcelas() + "-" + TipoDeSimulacao.FATOR_PARA_VALOR_NA_PARCELA,
                dadosSimulacaoSeguro);
    }

    private void prepararSimulacaoDoValorDoBem(Prazo prazo,
                                               CustoPercentualDoSeguroPrestamista custoPercentualDoSeguroPrestamista,
                                               BigDecimal valorDoSeguroPrestamista,
                                               List<ItemFinanciavel> itensFinanciaveis,
                                               CalculoSeguroMecanicaResponse seguroGarantia) {
        List<ParcelaBalao> parcelasBalao = getParcelasBalao(prazo);
        SimulacaoParcResidualRequest request = dados.getRequest();
        IOF iof = dados.getDadosCalculo().getTaxasIOF();
        BigDecimal valorBemComSeguroAuto = request.getValorBem().add(dados.getRequest().getValorSeguroAuto());
        TipoCalculo tipoCalculo;
        BigDecimal valorDoSubsidio = request.getValorSubsidio();
        if (valorDoSubsidio != null) {
            tipoCalculo = TipoCalculo.SUBSIDIO;
            BigDecimal limiteMaximo = limitesSubsidioValor.getLimitePorPrazo().get(prazo.getNumeroDeParcelas());
            if (valorDoSubsidio.compareTo(limiteMaximo) > 0) {
                valorDoSubsidio = limiteMaximo;
            }
        } else {
            tipoCalculo = TipoCalculo.TAXA;
        }

        DadosSimulacao dadosSimulacao = new DadosSimulacao(TipoDeSimulacao.RESIDUAL,
                dados.getDadosCalculo().getDataCalculo(),
                request.getDataPrimeiroVencimento(dados.getDadosCalculo().getDataCalculo()), valorBemComSeguroAuto, request.getVlrSeguroFranquia(),
                request.getValorEntrada(), valorDoSeguroPrestamista, dados.getDadosCalculo().getValorSeguroGarantia(), seguroGarantia, custoPercentualDoSeguroPrestamista,
                request.getValorUfEmplacamento(), request.getValorCestaServicos(), request.getValorTC(),
                request.getTaxaSubsidio(), request.getRetorno(), request.getPeriodicidade(), TipoFinanciamento.CDC,
                tipoCalculo, valorDoSubsidio, false, iof.getValorIOFAA(), iof.getValorIOFAD(),
                iof.getValorBaseCalculoParaIOF(), parcelasBalao, prazo, request.getTipoPessoa(), itensFinanciaveis, !request.getNDiasAposVencimento(),
                prazo.getTaxaBanco());
        dadosSimulacaoMap.put(prazo.getNumeroDeParcelas(), dadosSimulacao);
    }

    private List<ItemFinanciavel> obterItensFinanciaveis(List<ItemFinanciavel> itensFinanciaveis) {
        itensFinanciaveis = Objects.nonNull(itensFinanciaveis) ? itensFinanciaveis : new ArrayList<>();

        // Remove o item do serviço geolocalização para adicionar no cálculo automaticamente
        return itensFinanciaveis.stream().filter(item -> !Objects.equals(item.getCodigo(), EnumItemFinanciavel.SERVICO_GEOLOCALIZACAO.getKey())).collect(Collectors.toList());
    }

    private CalculoSeguroMecanicaResponse obterSeguroGarantiaMecanica(Prazo prazo) {
        if (dados.getRequest().getParametrosSeguroMecanica() != null && dados.getRequest().getParametrosSeguroMecanica().getCodigoFipe() != null && dados.getRequest().getParametrosSeguroMecanica().getQuilometragem() != null) {
            return calculadoraSeguroMecanicaServices.calcular(dados.getRequest().getParametrosSeguroMecanica(), prazo.getNumeroDeParcelas());
        }

        return null;
    }


    private BigDecimal obterValorSeguroGarantiaMecanica(CalculoSeguroMecanicaResponse seguroGarantiaMecanica) {
        BigDecimal valorSeguroGarantiaMecanica = BigDecimal.ZERO;

        if (Objects.nonNull(seguroGarantiaMecanica)
            && Objects.nonNull(seguroGarantiaMecanica.getSelecionado())
            && Objects.nonNull(seguroGarantiaMecanica.getSelecionado().getValorTotal())
            && seguroGarantiaMecanica.getSelecionado().getValorTotal().compareTo(BigDecimal.ZERO) > 0) {
            valorSeguroGarantiaMecanica = seguroGarantiaMecanica.getSelecionado().getValorTotal();
        }

        return valorSeguroGarantiaMecanica;
    }

}
