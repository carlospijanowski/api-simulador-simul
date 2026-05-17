package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.response.Fluxo;
import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.entities.TipoFinanciamento;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import br.com.bancotoyota.simuladorpropostasservices.entidades.FluxoFinanceiro;
import br.com.bancotoyota.simuladorpropostasservices.entidades.simulacao.ResultadoCalculado;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class FluxoService {

    private static class Loader {
        static final FluxoService INSTANCE = new FluxoService();
    }
    private FluxoService() {}

    public static FluxoService getInstance() {
        return Loader.INSTANCE;
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(Constants.FORMATO_DATA_BRASIL);

    public List<Fluxo> getFluxo(ResultadoCalculado resultadoCalculado, BigDecimal valorEntrada, LocalDate dataCalculo) {
        List<FluxoFinanceiro> listaFluxoFinanceiroComIOF = resultadoCalculado.getListaFluxoFinanceiroComIOF();
        BigDecimal valorParcela = getValorDaParcela(resultadoCalculado).setScale(2, RoundingMode.DOWN);
        return getFluxo(valorParcela, listaFluxoFinanceiroComIOF, valorEntrada, dataCalculo);
    }

    private List<Fluxo> getFluxo(BigDecimal valorParcela, List<FluxoFinanceiro> listaFluxoFinanceiroComIOF,
                                 BigDecimal valorEntrada, LocalDate dataCalculo) {
        List<Fluxo> list = listaFluxoFinanceiroComIOF.stream().map(fluxoFinanceiro -> new Fluxo(Integer.valueOf(fluxoFinanceiro.getIdParcela()),
                fluxoFinanceiro.getDataVencimento(),
                valorParcela,
                NumberUtils.toBigDecimal(fluxoFinanceiro.getPmtTotal()).subtract(valorParcela))
        ).collect(Collectors.toList());
        list.add(0, new Fluxo(0, FORMATTER.format(dataCalculo), valorEntrada, BigDecimal.ZERO));
        return list;
    }

    /**
     * Retorna o valor com ou sem IOF conforme configuração do produto
     */
    public BigDecimal getValorDaParcela(ResultadoCalculado resultadoCalculadoEx) {
        return getValorDaParcelaComOuSemIOF(resultadoCalculadoEx);
    }

    public BigDecimal getValorDaParcelaComOuSemIOF(ResultadoCalculado resultadoCalculadoEx) {
        BigDecimal valorParcela;
        if (resultadoCalculadoEx.getDadosSimulacao().getProduto().equals(TipoFinanciamento.CDC.getSistema())) {
            valorParcela = NumberUtils.toBigDecimal(resultadoCalculadoEx.getValorParcelaComIOF());
        } else {
            valorParcela = NumberUtils.toBigDecimal(resultadoCalculadoEx.getValorParcelaSemIOF());
        }
        return valorParcela;
    }
}
