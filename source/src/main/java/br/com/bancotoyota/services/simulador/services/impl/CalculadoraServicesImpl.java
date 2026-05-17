package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.*;
import br.com.bancotoyota.services.simulador.beans.response.*;
import br.com.bancotoyota.services.simulador.common.*;
import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.simuladorpropostasservices.entidades.ParcelaBalao;
import br.com.bancotoyota.simuladorpropostasservices.entidades.*;
import br.com.bancotoyota.simuladorpropostasservices.entidades.simulacao.*;
import br.com.bancotoyota.simuladorpropostasservices.services.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.*;
import org.apache.commons.lang3.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import java.math.*;
import java.text.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

/**
 * @author Luis Santos Implementação do serviço da calculadora de financiamento.
 */
@Service
@Slf4j
public class CalculadoraServicesImpl implements CalculadoraServices {
    private static final String DATA_FORMATO = "yyyyMMdd";

    private static final Runtime runtime = Runtime.getRuntime();
    private static final NumberFormat numberFormat = NumberFormat.getNumberInstance();

    /**
     * Usado para armazenar instancia da classe ServicesSimulacao para que apenas
     * uma instância seja criada por request.
     */
    @Autowired
    private HttpServletRequest httpRequest;

    private static List<ServicesSimulacao> listParaTesteDeMemoria;

    /**
     * Usado para testar o consumo de memória da planilha, deve ser null no
     * OpenShift
     */
    private static List<ServicesSimulacao> getListParaTesteDeMemoria() {
        return listParaTesteDeMemoria;
    }

    public static void setListParaTesteDeMemoria(List<ServicesSimulacao> listParaTesteDeMemoria) {
        CalculadoraServicesImpl.listParaTesteDeMemoria = listParaTesteDeMemoria;
    }

    /**
     * Uma vez que a classe ServicesSimulacao não é thread safe, não podemos usar
     * apenas uma instância para todos os requests, por outro lado também não
     * queremos criar novas instancias para mais de uma chamada no mesmo request.
     * Por isso armazenamos o objeto no request HTTP.
     */
    private ServicesSimulacao getServicesSimulacao() {
        if (httpRequest != null) {
            ServicesSimulacao servicesSimulacao = (ServicesSimulacao) httpRequest
                    .getAttribute(ServicesSimulacao.class.getName());
            if (servicesSimulacao == null) {
                servicesSimulacao = new ServicesSimulacao();
                httpRequest.setAttribute(ServicesSimulacao.class.getName(), servicesSimulacao);
                log.info("memória usada pelo serviço: " + numberFormat.format(runtime.totalMemory()));
            }
            return servicesSimulacao;
        } else {
            ServicesSimulacao s = new ServicesSimulacao();
            if (getListParaTesteDeMemoria() != null) {
                getListParaTesteDeMemoria().add(s);
            }
            return s;
        }
    }

    @Override
    public ResultadoCalculado calcular(DadosSimulacao dadosSimulacao) {
        DadosSimulacaoPlanilha dadosSimulacaoPlanilha = getDadosSimulacaoPlanilha(dadosSimulacao);
        String idCall = "1";
        long startTime = System.currentTimeMillis();
        try {
            return getServicesSimulacao().realizaSimulacao(TipoPlanilha.BTB, idCall, dadosSimulacaoPlanilha);
        } finally {
            long endTime = System.currentTimeMillis();
            if (dadosSimulacao.getTipoCalculo() == TipoCalculo.SUBSIDIO) {
                log.warn("tempo de execução da planilha com subsidio por valor para um elemento: "
                        + (endTime - startTime));
            } else {
                log.debug("tempo de execução da planilha para um elemento: " + (endTime - startTime));
            }
        }
    }

    @Override
    public List<ResultadoCalculado> calcular(Collection<DadosSimulacao> dadosSimulacoes) {
        List<DadosSimulacaoPlanilha> dadosSimulacoesPlanilha = dadosSimulacoes.stream()
                .map(this::getDadosSimulacaoPlanilha).collect(Collectors.toList());
        // Não podemos passar para a planilha simulações com valor do bem igual a zero
        // (da um erro na planilha)...

        boolean temCalculoDeSubsidioPorValor = false;
        for (DadosSimulacao ds : dadosSimulacoes) {
            if (ds.getTipoCalculo() == TipoCalculo.SUBSIDIO) {
                temCalculoDeSubsidioPorValor = true;
            }
        }
        String idCall = "1";
        long startTime = System.currentTimeMillis();
        List<ResultadoCalculado> resultadosCalculados = getServicesSimulacao().realizaSimulacoes(TipoPlanilha.BTB,
                idCall, dadosSimulacoesPlanilha);
        long endTime = System.currentTimeMillis();

        if (temCalculoDeSubsidioPorValor) {
            log.warn("tempo de execução da planilha com subsidio por valor para " + dadosSimulacoes.size()
                    + " elementos: " + (endTime - startTime));
        } else {
            log.debug("tempo de execução da planilha para " + dadosSimulacoes.size() + " elementos: "
                    + (endTime - startTime));
        }

        return resultadosCalculados;
    }

    /**
     * Converte os parâmetros da nossa estrutura DadosSimulacao para a estrutura
     * DadosSimulacaoPlanilha
     *
     * @param dadosSimulacao dados da simulação.
     * @return
     */
    @Override
    public DadosSimulacaoPlanilha getDadosSimulacaoPlanilha(DadosSimulacao dadosSimulacao) {

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATA_FORMATO);

        BigDecimal taxaBanco = dadosSimulacao.getPrazo().getTaxaBanco().setScale(6, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100));

        BigDecimal taxaOperacao = null;
        if (dadosSimulacao.getValorTaxaSubsidio() != null) {
            taxaOperacao = dadosSimulacao.getValorTaxaSubsidio().divide(BigDecimal.valueOf(100), 8,
                    RoundingMode.HALF_UP);

            if (taxaOperacao.compareTo(taxaBanco) >= 0) {
                taxaOperacao = taxaBanco.subtract(Constants.UM_CENTESIMO_PORCENTO);
            }
        }

        BigDecimal valorCestaServico = (dadosSimulacao.getValorCestaServico() == null) ? BigDecimal.ZERO
                : dadosSimulacao.getValorCestaServico();
        BigDecimal valorTC = (dadosSimulacao.getValorTC() == null) ? BigDecimal.ZERO : dadosSimulacao.getValorTC();

        List<ParcelaBalao> parcelasBalao = new ArrayList<>();
        if (dadosSimulacao.getParcelasBalao() != null) {
            parcelasBalao = dadosSimulacao.getParcelasBalao().stream()
                    .map(parcelaBalao -> new ParcelaBalao(parcelaBalao.getValor().toString(),
                            parcelaBalao.getData().format(dateTimeFormatter)))
                    .collect(Collectors.toList());
        }
        BigDecimal valorSeguroFranquia = dadosSimulacao.getVlrSeguroFranquia();
        BigDecimal valorSeguroPrestamista = dadosSimulacao.getValorSeguroPrestamista();
        BigDecimal valorSeguro = BigDecimal.ZERO;
        BigDecimal valorSeguroGarantia = Optional.ofNullable(dadosSimulacao.getDadosSeguroMecanica())
                .map(CalculoSeguroMecanicaResponse::getSelecionado)
                .map(SeguroMecanica::getValorTotal)
                .or(() -> Optional.ofNullable(dadosSimulacao.getValorSeguroGarantia()))
                .orElse(BigDecimal.ZERO);

        if (valorSeguroFranquia != null) {
            valorSeguro = valorSeguroFranquia;
        }
        if (valorSeguroPrestamista != null) {
            valorSeguro = valorSeguro.add(valorSeguroPrestamista);
        }
        valorSeguro = valorSeguro.add(valorSeguroGarantia);

        BigDecimal valorAcessorios = dadosSimulacao.getValorAcessorios();

        return new DadosSimulacaoPlanilha(dadosSimulacao.getIdSimulacao().toString(), "1", // numeroProposta,
                dadosSimulacao.getTaxaIOFAA().toString(), dadosSimulacao.getTaxaIOFAD().toString(),
                taxaBanco.toString(), dadosSimulacao.getPeriodicidade().toString(),
                dadosSimulacao.getTipoFinanciamento().getSistema(), taxaOperacao != null ? taxaOperacao.toString() : "",
                (dadosSimulacao.getValorSubsidio() == null) ? "0" : dadosSimulacao.getValorSubsidio().toString(),
                dadosSimulacao.getPrazo().getNumeroDeParcelas().toString(), dadosSimulacao.getCodigoRetorno(),
                valorCestaServico.toString(), valorTC.toString(),
                (dadosSimulacao.getValorRegistroContrato() == null) ? "0"
                        : dadosSimulacao.getValorRegistroContrato().toString(),
                dadosSimulacao.getDataSimulacao().format(dateTimeFormatter),
                dadosSimulacao.getDataPrimeiroVencimento().format(dateTimeFormatter),
                dadosSimulacao.getValorBem().toString(),
                valorSeguro.toString(),
                (dadosSimulacao.getValorEntrada() == null) ? "0" : dadosSimulacao.getValorEntrada().toString(),
                (dadosSimulacao.isTemIsencaoIOF()) ? "S" : "N", "0", dadosSimulacao.getBaseCalculoIOF().toString(),
                parcelasBalao, (valorAcessorios == null) ? "0" : valorAcessorios.toString(),
                dadosSimulacao.getTipoCalculo().name(), BooleanUtils.isTrue(dadosSimulacao.getUsuarioInformouDataPrimeiroVencimento()),
                dadosSimulacao.getTaxaBanco());
    }
}
