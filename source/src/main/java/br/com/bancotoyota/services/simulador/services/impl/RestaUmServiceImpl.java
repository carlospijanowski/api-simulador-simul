package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.request.*;
import br.com.bancotoyota.services.simulador.beans.response.*;
import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.controller.SimuladorParcDesejadaController;
import br.com.bancotoyota.services.simulador.controller.SimuladorParcDesejadaNovaController;
import br.com.bancotoyota.services.simulador.controller.SimuladorParcResidualController;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.entities.Modalidade;
import br.com.bancotoyota.services.simulador.entities.TipoPessoa;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RestaUmServiceImpl implements RestaUmService {

    @Autowired
    private FeatureToggleConfig featureToggleConfig;
    private EmplacamentoService emplacamentoService;
    private OrigemNegocioService origemNegocioService;
    private CestaServicoService cestaServicoService;
    private PlanoService planoService;

    private MotorTaxaPlanoService motorTaxaPlanoService;
    private ClientService clientService;

    @Value("${simulacao-simplificada-parceiros.enabled:false}")
    private Boolean simulacaoSimplificadaParceirosEnabled;

    @Setter
    @Autowired(required = false)
    private SimuladorParcDesejadaController simuladorParcDesejadaController;

    @Setter
    @Autowired(required = false)
    private SimuladorParcDesejadaNovaController simuladorParcDesejadaNovaController;

    private SimuladorParcResidualController simuladorParcResidualController;

    @Autowired
    public RestaUmServiceImpl(EmplacamentoService emplacamentoService,
                              OrigemNegocioService origemNegocioService,
                              CestaServicoService cestaServicoService,
                              PlanoService planoService, MotorTaxaPlanoService motorTaxaPlanoService, ClientService clientService,
                              SimuladorParcResidualController simuladorParcResidualController, FeatureToggleConfig featureToggleConfig) {
        this.emplacamentoService = emplacamentoService;
        this.origemNegocioService = origemNegocioService;
        this.cestaServicoService = cestaServicoService;
        this.planoService = planoService;
        this.motorTaxaPlanoService = motorTaxaPlanoService;
        this.clientService = clientService;
        this.simuladorParcResidualController = simuladorParcResidualController;
        this.featureToggleConfig = featureToggleConfig;
    }

    @Override
    public SimulacaoRequest getSimulacaoRequest(SimulacaoSimplificadaRequest request, ConfiguracaoPlano configuracao,
                                                DadosAlternativa dadosAlternativa, String perfilUsuario, String token, SimulacaoSimplificada simulacaoSimplificada, List<String> idsModalidade) {

        OrigemNegocio origemNegocio = origemNegocioService.getOrigemNegocio(request.getCnpjOrigemNegocio(), token);

        // para as cestas a modalidade é sempre CDC
        CestaServico cestaServico = cestaServicoService.getCestaServicoMaisCara(request.getCnpjOrigemNegocio(),
                Modalidade.CDC, configuracao.getTipoPessoa(), perfilUsuario, token);
        dadosAlternativa.setCesta(cestaServico);

        List<CestaItem> cestaItems = cestaServicoService.getDespesas(cestaServico.getCodigo(), token);
        cestaServico.setCestaItems(cestaItems);
        dadosAlternativa.setCestaItens(cestaItems);

        FiltrosPlano filtrosPlano = FiltrosPlano.builder()
                .configuracao(configuracao)
                .cnpjOrigemNegocio(origemNegocio.getCnpj())
                .regiao(origemNegocio.getCodigoRegiao())
                .marca(request.getMarcaId())
                .modelo(request.getModeloId())
                .anoModelo(request.getAnoModelo())
                .perfilUsuario(perfilUsuario)
                .workaroundObo(perfilUsuario == null)
                .idsModalidade(idsModalidade)
                .aplicacao(request.getOrigemSolicitacao())
                .build();

        Plano plano;
        if(BooleanUtils.isTrue(featureToggleConfig.getBuscaApiMotorTaxaEnabled())) {
            plano = motorTaxaPlanoService.getPlanoCicloToyota(request.getValorBem(), request.getValorEntrada(), filtrosPlano);
        } else {
            plano = planoService.getPlanoCicloToyota(request.getValorBem(), request.getValorEntrada(), filtrosPlano, token);
        }
        dadosAlternativa.setPlano(plano);
        log.info("Plano selecionado: " + plano.getCodigoPlano());
        Retorno retorno = getRetornoDoPlanoFinanciamento(request, perfilUsuario, token, origemNegocio, plano);
        dadosAlternativa.setRetorno(retorno);

        Estado estado = getEstadoEmplacamento(request, token, origemNegocio);
        BigDecimal valorSeguroFranquia = request.getValorSeguroFranquia() != null ? request.getValorSeguroFranquia() : BigDecimal.ZERO;
        SimulacaoRequest simulacaoRequest = new SimulacaoRequest();
        simulacaoRequest.setVlrSeguroFranquia(valorSeguroFranquia);
        simulacaoRequest.setValorBem(request.getValorBem());
        simulacaoRequest.setValorEntrada(request.getValorEntrada());

        simulacaoRequest.setTipoPessoa(configuracao.getTipoPessoa().getValue().toLowerCase());

        simulacaoRequest.setValorUfEmplacamento(estado.getValor());
        simulacaoRequest.setValorCestaServicos(cestaServico.getValorSemTaxaCadastro());

        determinaTaxaCadastro(request, cestaServico, simulacaoRequest);

        Carencia carencia = new Carencia();
        carencia.setMaximo(plano.getQuantidadeMaximaCarencia());
        carencia.setMinimo(plano.getQuantidadeMinimaCarencia());
        simulacaoRequest.setCarenciaDoPlano(carencia);


        Saida saida = new Saida();
        if (configuracao.getTipoPessoa() == TipoPessoa.PESSOA_FISICA
                || configuracao.getTipoPessoa() == TipoPessoa.PESSOA_JURIDICA) {
            ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista();

            boolean pessoaFisica = configuracao.getTipoPessoa() == TipoPessoa.PESSOA_FISICA;
            parametrosDeSeguroPrestamista.setSpfDisponivel(pessoaFisica && origemNegocio.getSpfDisponivel());
            parametrosDeSeguroPrestamista.setSvpDisponivel(origemNegocio.getSvpDisponivel());
            parametrosDeSeguroPrestamista.setSvpSeSpfRemovido(origemNegocio.getSvpSeSpfRemovido());
            parametrosDeSeguroPrestamista.setCnpjOrigemNegocio(origemNegocio.getCnpj());
            parametrosDeSeguroPrestamista.setCpfCnpjCliente(request.getCpfCnpjCliente());
            parametrosDeSeguroPrestamista.setCanalOrigem("DIRECT");
            parametrosDeSeguroPrestamista.setChaveOrigem("SIMUL-" + System.currentTimeMillis());

            parametrosDeSeguroPrestamista.setSpfRemovido(
                    request.getSpfRemovido() != null && request.getSpfRemovido());
            parametrosDeSeguroPrestamista.setSvpRemovido(
                    request.getSvpRemovido() != null && request.getSvpRemovido());

            simulacaoRequest.setParametrosDeSeguroPrestamista(parametrosDeSeguroPrestamista);

            ValorComprometidoSeguroPrestamista.ClienteComprometimentoSeguro clienteComprometimentoSeguro = null;
            if (pessoaFisica && request.getCpfCnpjCliente() != null) {
                clienteComprometimentoSeguro = clientService.getValoresComprometidos(request.getCpfCnpjCliente());
            }
            if (clienteComprometimentoSeguro != null) {
                parametrosDeSeguroPrestamista.setSpfValorComprometido(clienteComprometimentoSeguro.getSpfValorComprometido());
                parametrosDeSeguroPrestamista.setSvpValorComprometido(clienteComprometimentoSeguro.getSvpValorComprometido());
            }

            //Variável que será gravada no MongoDB
            saida.setParametrosSeguroPrestamista(parametrosDeSeguroPrestamista);
        }

        simulacaoRequest.setPlanoId(Integer.parseInt(plano.getCodigoPlano()));
        simulacaoRequest.setPlanoCiclo(plano);

        simulacaoRequest.setIsentaIOF(plano.getIsentaIof());
        simulacaoRequest.setRetorno(retorno.getCodigo());
        simulacaoRequest.setSeguroAuto(request.getSeguroAuto());
        simulacaoRequest.setDataPrimeiroVencimento(request.getDataPrimeiroVencimento());
        simulacaoRequest.setParametrosSeguroMecanica(request.getParametrosSeguroMecanica());
        simulacaoRequest.setNDiasAposVencimento(isNDiasAposVencimento(request));

        simulacaoRequest.setItens(request.getItens());

        BigDecimal taxaSubsidio = getTaxaSubsidio(request, configuracao, plano);
        simulacaoRequest.setTaxaSubsidio(taxaSubsidio);

        /*
         * Monta a estrutura da variável que será gravada no MongoDB
         */
        if (simulacaoSimplificada != null && simulacaoSimplificadaParceirosEnabled) {

            Entrada entrada = new Entrada();
            if (simulacaoSimplificada.getEntrada() != null) {
                entrada = simulacaoSimplificada.getEntrada();
            }

            Dealer dealer = new Dealer(origemNegocio.getCnpj(), origemNegocio.getCodigo(), origemNegocio.getCidade(), origemNegocio.getEstado());
            entrada.setDealer(dealer);

            DetalheSimulacao detalheSimulacao = new DetalheSimulacao();
            detalheSimulacao.setDataPrimeiroVencimento(request.getDataPrimeiroVencimento());
            detalheSimulacao.setModalidadeId(configuracao.getModalidade().getValue());
            detalheSimulacao.setSituacaoVeiculo(request.getSituacaoVeiculo().getValue());
            detalheSimulacao.setTaxaSubsidio(request.getTaxaSubsidio());
            detalheSimulacao.setTipoPessoa(configuracao.getTipoPessoa().getValue());
            detalheSimulacao.setUfEmplacamento(estado.getUf());
            detalheSimulacao.setValorEntrada(request.getValorEntrada());
            detalheSimulacao.setSeguroAuto(request.getSeguroAuto());
            detalheSimulacao.setCodigoRetorno(request.getCodigoRetorno());
            detalheSimulacao.setAnoModelo(request.getAnoModelo());

            entrada.setDetalheSimulacao(detalheSimulacao);
            entrada.setItensFinanciaveis(simulacaoRequest.getItens());
            entrada.setParcelas(simulacaoRequest.getParcelas());
            simulacaoSimplificada.setEntrada(entrada);

            saida.setPlano(plano);
            saida.setCestaServico(cestaServico);
            saida.setEmplacamento(estado);
            log.info("SAIDA EMPLACAMENTO {}", saida.getEmplacamento().getUf());
            saida.setCodigoRetorno(request.getCodigoRetorno());
            simulacaoSimplificada.setSaida(saida);
        }
        return simulacaoRequest;
    }

    private BigDecimal getTaxaSubsidio(SimulacaoSimplificadaRequest request, ConfiguracaoPlano configuracao, Plano plano) {
        if (configuracao.isSubsidio() && request.getTaxaSubsidio() == null && plano.getTipoSubsidio().equals("V") && configuracao.getPlanoOrigianl() >= 0) {
            // Não passamos a taxa quando o plano é do tipo subsídio fixo e se o plano escolhido é de subsídio variável
            // precisamos pegar a taxa para ser usada.
            List<Prazo> prazos;
            if(BooleanUtils.isTrue(featureToggleConfig.getBuscaApiMotorTaxaEnabled())) {
                prazos = motorTaxaPlanoService.findPrazos(configuracao.getPlanoOrigianl(), null);
            } else {
                prazos = planoService.findPrazos(configuracao.getPlanoOrigianl(), null);
            }

            if (prazos != null) {
                // vamos pegar a taxa para o maior prazo do plano original
                Prazo maiorPrazoOriginal = prazos.get(prazos.size() - 1);
                return maiorPrazoOriginal.getTaxaBanco();
            }
        } else {
            return request.getTaxaSubsidio();
        }
        return null;
    }

    private boolean isNDiasAposVencimento(SimulacaoSimplificadaRequest request) {
        return request.getDataPrimeiroVencimento() == null;
    }

    private Estado getEstadoEmplacamento(SimulacaoSimplificadaRequest request, String token, OrigemNegocio origemNegocio) {
        /*
         * Atribui o Estado da Origem de Negócio quando o estado não é enviado no request
         * */
        Estado estado;
        if (request.getUfEmplacamento() != null) {
            estado = emplacamentoService.getEstado(request.getUfEmplacamento(), token);
        } else {
            estado = emplacamentoService.getEstado(UfEmplacamento.fromValue(origemNegocio.getEstado()), token);
            request.setUfEmplacamento(UfEmplacamento.fromValue(origemNegocio.getEstado()));
        }
        return estado;
    }

    private Retorno getRetornoDoPlanoFinanciamento(SimulacaoSimplificadaRequest request, String perfilUsuario, String token, OrigemNegocio origemNegocio, Plano plano) {
        Retorno retorno;
        if (isSimplificadaLigada(request)) {
            if(BooleanUtils.isTrue(featureToggleConfig.getBuscaApiMotorTaxaEnabled())) {
                retorno = motorTaxaPlanoService.getRetorno(plano.getCodigoPlano(), origemNegocio.getCnpj(), perfilUsuario, request.getCodigoRetorno());
            } else {
                retorno = planoService.getRetorno(plano.getCodigoPlano(), origemNegocio.getCnpj(), perfilUsuario, request.getCodigoRetorno(), token);
            }

        } else {
            request.setCodigoRetorno(Constants.RETORNO_DEFAULT);
            if(BooleanUtils.isTrue(featureToggleConfig.getBuscaApiMotorTaxaEnabled())) {
                retorno = motorTaxaPlanoService.getRetorno(plano.getCodigoPlano(), origemNegocio.getCnpj(), perfilUsuario);
            } else {
                retorno = planoService.getRetorno(plano.getCodigoPlano(), origemNegocio.getCnpj(), perfilUsuario, token);
            }
        }
        return retorno;
    }

    private boolean isSimplificadaLigada(SimulacaoSimplificadaRequest request) {
        return request.getCodigoRetorno() != null && simulacaoSimplificadaParceirosEnabled;
    }

    private void determinaTaxaCadastro(SimulacaoSimplificadaRequest request, CestaServico cestaServico, SimulacaoRequest simulacaoRequest) {
        boolean isClienteAtivo = false;
        if (request.getCpfCnpjCliente() != null) {
            isClienteAtivo = clientService.isAtivo(request.getCpfCnpjCliente());
        }
        if (isClienteAtivo) {
            simulacaoRequest.setValorTC(BigDecimal.ZERO);
        } else {
            simulacaoRequest.setValorTC(cestaServico.getTaxaCadastro());
        }
    }

    public AlternativaResponse opcaoCicloToyota(RestaUmRequest request, String perfilUsuario, String token) {

        DadosAlternativa dadosAlternativa = new DadosAlternativa();
        StopWatch sw = new StopWatch("opção ciclo-toyota");

        SimulacaoRequest simulacaoRequest = getSimulacaoRequest(request, perfilUsuario, token, dadosAlternativa, sw);

        SimulacaoParcDesejadaRequest finalRequest = new SimulacaoParcDesejadaRequest(simulacaoRequest);
        finalRequest.setValorParcelaDesejada(request.getParametros().getValorParcelaDesejada());

        sw.start("simulação");
        SimulacaoParcDesejadaResponse responseSimulacao;
        if (simuladorParcDesejadaNovaController == null) {
            responseSimulacao = simuladorParcDesejadaController.getParcelaDesejadaImpl(finalRequest);
        } else {
            responseSimulacao = simuladorParcDesejadaNovaController.getParcelaDesejadaImpl(finalRequest);
        }
        sw.stop();

        responseSimulacao.setRequest(finalRequest);

        log.debug(sw.prettyPrint());

        return new AlternativaResponse(responseSimulacao, dadosAlternativa);
    }

    @Override
    public AlternativaPrazoResponse opcaoCicloToyotaAPrazo(RestaUmRequest request, String perfilUsuario, String token) {
        DadosAlternativa dadosAlternativa = new DadosAlternativa();
        StopWatch sw = new StopWatch("opção ciclo-toyota");
        SimulacaoRequest simulacaoRequest = getSimulacaoRequest(request, perfilUsuario, token, dadosAlternativa, sw);

        sw.start("simulação");
        BigDecimal valorMaximoBalao = calculaValorMaximoResidual(request, simulacaoRequest);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest = new SimulacaoParcResidualRequest(simulacaoRequest);
        simulacaoParcResidualRequest.setValorParcelaResidual(valorMaximoBalao);

        List<Prazo> prazoDisponivelCiclo = encontraPrazoDisponivelCiclo(request, simulacaoParcResidualRequest);

        SimulacaoParcResidualResponse simulacaoParcelaResidual = simuladorParcResidualController.getParcelaResidualImpl(simulacaoParcResidualRequest, prazoDisponivelCiclo);
        simulacaoParcResidualRequest.setPrazoDisponivelCiclo(prazoDisponivelCiclo);
        simulacaoParcelaResidual.setRequest(simulacaoParcResidualRequest);
        sw.stop();

        log.debug(sw.prettyPrint());

        return new AlternativaPrazoResponse(simulacaoParcelaResidual, dadosAlternativa);
    }

    private SimulacaoRequest getSimulacaoRequest(RestaUmRequest request, String perfilUsuario, String token, DadosAlternativa dadosAlternativa, StopWatch sw) {
        sw.start("criar request");
        SimulacaoRequest simulacaoRequest = getSimulacaoRequest(request.getParametros(), request.getConfiguracao(), dadosAlternativa, perfilUsuario, token, null, null);
        sw.stop();

        return simulacaoRequest;
    }

    private BigDecimal calculaValorMaximoResidual(RestaUmRequest request, SimulacaoRequest simulacaoRequest) {
        BigDecimal valorBem = request.getParametros().getValorBem();
        BigDecimal porcentagemMaximaBalao = simulacaoRequest.getPlanoCiclo().getParcelaMaximaBalao();
        BigDecimal valorMaximoBalao = porcentagemMaximaBalao.multiply(valorBem)
                .divide(NumberUtils.CEM);

        BigDecimal valorEntrada = request.getParametros().getValorEntrada();
        BigDecimal valorParcial =  valorEntrada.add(valorMaximoBalao);

        if (valorParcial.compareTo(valorBem) > 0) {
            valorParcial = valorParcial.subtract(valorBem);
            valorMaximoBalao = valorMaximoBalao.subtract(valorParcial);
        }

        return valorMaximoBalao;
    }

    private List<Prazo> encontraPrazoDisponivelCiclo(RestaUmRequest request, SimulacaoParcResidualRequest simulacaoParcResidualRequest) {
        List<Prazo> prazos = simuladorParcResidualController.carregarPrazos(simulacaoParcResidualRequest);

        if(request.getParametros().getParcelas() == null || request.getParametros().getParcelas().length == 0){
            log.error("Parcela não informada.");
            throw new EntityNotFoundException(new Erro("Parametros.parcelas","null", "Parcela não informada"));
        }

        final Integer prazoOriginal = request.getParametros().getParcelas()[0];
        final Prazo prazoMinimoCiclo = prazos.stream().min(Comparator.comparing(Prazo::getNumeroDeParcelas))
                .orElse(new Prazo());
        final Prazo prazoMaximoCiclo = prazos.stream().max(Comparator.comparing(Prazo::getNumeroDeParcelas))
                .orElse(new Prazo());

        Integer prazoSelecionado = prazoOriginal;
        if (prazoOriginal < prazoMinimoCiclo.getNumeroDeParcelas()) {
            prazoSelecionado = prazoMinimoCiclo.getNumeroDeParcelas();
        } else if (prazoOriginal > prazoMaximoCiclo.getNumeroDeParcelas()) {
            prazoSelecionado = prazoMaximoCiclo.getNumeroDeParcelas();
        }

        Collection<Integer> meses = Arrays.asList(prazoSelecionado);
        List<Prazo> prazoFiltrado = prazos.stream()
                .filter(p -> meses.contains(p.getNumeroDeParcelas()))
                .collect(Collectors.toList());

        log.error("Minimo: " + prazoMinimoCiclo.getNumeroDeParcelas() + " | Maximo: " + prazoMaximoCiclo.getNumeroDeParcelas());
        log.error("Selecionado: " + prazoSelecionado);

        return prazoFiltrado;
    }
}
