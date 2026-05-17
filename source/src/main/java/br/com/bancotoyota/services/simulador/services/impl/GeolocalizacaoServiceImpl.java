package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.ItemFinanciavel;
import br.com.bancotoyota.services.simulador.beans.geolocalizacao.DadosCobrancaServicoGeolocalizacao;
import br.com.bancotoyota.services.simulador.beans.geolocalizacao.ParametrizacaoCobrancaGeo;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.entities.EnumItemFinanciavel;
import br.com.bancotoyota.services.simulador.entities.Plano;
import br.com.bancotoyota.services.simulador.services.GeolocalizacaoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class GeolocalizacaoServiceImpl implements GeolocalizacaoService {
    @Autowired
    FeatureToggleConfig featureToggleConfig;

    @Autowired
    public GeolocalizacaoServiceImpl(FeatureToggleConfig featureToggleConfig) {
        this.featureToggleConfig = featureToggleConfig;
    }
    @Override
    public DadosCobrancaServicoGeolocalizacao obterDadosCobrancaServicoGeolocalizacao(SimulacaoParcResidualRequest request, Plano plano, Integer prazoSelecionado) {
        // Geolocalização só existe para planos do condigurador (que são extraídos pelo Motor de Taxas)
        if (featureToggleConfig != null && featureToggleConfig.getBuscaApiMotorTaxaEnabled()) {
            // Plano é conectado, porém deve ter parâmetros de geolocalização.
            if (prazoSelecionado != null &&
                plano != null &&
                plano.getHabilitaGeoLocalizacao() != null &&
                plano.getHabilitaGeoLocalizacao() &&
                plano.getParametrosGeoLocalizacao() != null &&
                plano.getParametrosGeoLocalizacao().getPermiteDispositivoGeo() &&
                plano.getParametrosGeoLocalizacao().getParametros() != null) {

                Boolean contratouSeguro = request.getSeguroAutoIntegrado() != null && request.getSeguroAutoIntegrado().getIdCotacao() != null && !request.getSeguroAutoIntegrado().getIdCotacao().isEmpty(); // Se contratou o seguro
                boolean contratoSeguroIgualFinanciamento = contratouSeguro && (prazoSelecionado.equals(request.getSeguroAutoIntegrado().getPrazoContratado())); // Se financiamento acaba depois do contrato de seguro ou junto dele

                BigDecimal valorServicoGeoLocalizacao = plano.getParametrosGeoLocalizacao().getValorServicoGeoLocalizacao();

                // Descobre em qual das faixas de prazo se encaixa o prazo selecionado se encontra.
                // Além disso, verifica se plano contratou seguro e se o financiamento acaba junto ou depois do período d o contrato.
                ParametrizacaoCobrancaGeo parametrizacaoEncontrada = null;

                List<ParametrizacaoCobrancaGeo> parametrizacaoPlano = plano.getParametrosGeoLocalizacao().getParametros();
                if (contratouSeguro) {
                    boolean possuiParametrizacaoSeguradora = parametrizacaoPlano.stream().anyMatch(p -> Objects.nonNull(p.getCnpjSeguradora()) && p.getCnpjSeguradora().equals(request.getSeguroAutoIntegrado().getCnpjSeguradora()));
                    if (!possuiParametrizacaoSeguradora) {
                        contratouSeguro = false;
                    }
                }

                for (ParametrizacaoCobrancaGeo parametrizacaoCobrancaGeo : parametrizacaoPlano) {
                    if (parametrizacaoCobrancaGeo.getInicio() <= prazoSelecionado && prazoSelecionado <= parametrizacaoCobrancaGeo.getFim()
                        && parametrizacaoCobrancaGeo.getContratou().equals(contratouSeguro)
                        && (contratouSeguro.equals(false) ||
                            (
                                contratouSeguro.equals(true) &&
                                parametrizacaoCobrancaGeo.getCnpjSeguradora().equals(request.getSeguroAutoIntegrado().getCnpjSeguradora()) &&
                                parametrizacaoCobrancaGeo.getPeriodoDoContrato().equals(contratoSeguroIgualFinanciamento)
                            )
                        )
                    ) {
                        parametrizacaoEncontrada = parametrizacaoCobrancaGeo;
                        break;
                    }
                }

                // De posse dos dados de geolocalização para a faixa em que se encaixa o prazo, vamos às formulas
                if (parametrizacaoEncontrada != null) {
                    // Variáveis gerais para o cálculo, usadas ao longo das 3 fórmulas abaixo.
                    int qtdMesesSubsidioSeguradora = 0;
                    int qtdMesesSubsidioTDB = 0;
                    int qtdMesesSubsidioBTB = 0;
                    int qtdMesesCobradoDoCliente = 0;
                    BigDecimal valorCobradoCliente = new BigDecimal(BigInteger.ZERO);
                    BigDecimal valorRepasseBTB = new BigDecimal(BigInteger.ZERO);
                    boolean deveCriarItemFinanciavel = false;

                    if (contratouSeguro && !parametrizacaoEncontrada.getCnpjSeguradora().equals(request.getSeguroAutoIntegrado().getCnpjSeguradora())) {
                        contratouSeguro = false;
                        contratoSeguroIgualFinanciamento = false;
                    }

                    if (!contratouSeguro) { // Fórmula 1: Se não contratou seguro.
                        qtdMesesSubsidioTDB = Math.min(parametrizacaoEncontrada.getTdb(), prazoSelecionado);

                        int qtdMesesSubsidioBTBTemp = prazoSelecionado - parametrizacaoEncontrada.getTdb();
                        qtdMesesSubsidioBTB = prazoSelecionado >= (parametrizacaoEncontrada.getBtb() + qtdMesesSubsidioTDB) ? parametrizacaoEncontrada.getBtb() : Math.max(qtdMesesSubsidioBTBTemp, 0);

                        int prazoClienteTemp = prazoSelecionado - (qtdMesesSubsidioTDB + qtdMesesSubsidioBTB);
                        qtdMesesCobradoDoCliente = Math.max(prazoClienteTemp, 0);

                        valorCobradoCliente = valorServicoGeoLocalizacao.multiply(new BigDecimal(qtdMesesCobradoDoCliente));
                        valorRepasseBTB = valorServicoGeoLocalizacao.multiply(new BigDecimal(qtdMesesSubsidioBTB));
                        deveCriarItemFinanciavel = true;
                    } else if (!contratoSeguroIgualFinanciamento) { // Fórmula 2: Contratou o seguro, porém não respeita o prazo do contrato (sai antes de completar o período do contrato)

                        qtdMesesSubsidioBTB = parametrizacaoEncontrada.getBtb();
                        qtdMesesSubsidioTDB = parametrizacaoEncontrada.getTdb();

                        if (prazoSelecionado >= parametrizacaoEncontrada.getSeguradora() + qtdMesesSubsidioTDB) {
                            qtdMesesSubsidioSeguradora = parametrizacaoEncontrada.getSeguradora();
                        } else {
                            qtdMesesSubsidioSeguradora = prazoSelecionado - qtdMesesSubsidioTDB;
                        }

                        if (prazoSelecionado <= (qtdMesesSubsidioTDB + qtdMesesSubsidioBTB + qtdMesesSubsidioSeguradora)) {
                            qtdMesesCobradoDoCliente = 0;
                        } else {
                            qtdMesesCobradoDoCliente = prazoSelecionado - (qtdMesesSubsidioBTB + qtdMesesSubsidioTDB + qtdMesesSubsidioSeguradora);
                        }

                        valorCobradoCliente = valorServicoGeoLocalizacao.multiply(new BigDecimal(qtdMesesCobradoDoCliente));
                        valorRepasseBTB = BigDecimal.ZERO;
                        deveCriarItemFinanciavel = true;
                    } else if (contratoSeguroIgualFinanciamento) { // Fórmula 3: Contratou o seguro, porém respeita o prazo do contrato. AQUI NÃO CRIA ITEM FINANCIÁVEL.
                        qtdMesesSubsidioTDB = parametrizacaoEncontrada.getTdb();

                        qtdMesesSubsidioSeguradora = prazoSelecionado >= (parametrizacaoEncontrada.getSeguradora() + qtdMesesSubsidioTDB)
                                ? parametrizacaoEncontrada.getSeguradora()
                                : prazoSelecionado - qtdMesesSubsidioTDB;

                        valorCobradoCliente = BigDecimal.ZERO;
                        valorRepasseBTB = BigDecimal.ZERO;
                    }

                    // Adiciona na request elementos do cálculo feito, para repassar para a proposta.
                    DadosCobrancaServicoGeolocalizacao dadosCobrancaServicoGeolocalizacao = new DadosCobrancaServicoGeolocalizacao();

                    if (deveCriarItemFinanciavel && valorCobradoCliente.compareTo(BigDecimal.ZERO) > 0) {
                        ItemFinanciavel itemFinanciavelGeolocalizacao = new ItemFinanciavel(EnumItemFinanciavel.SERVICO_GEOLOCALIZACAO.getKey(),EnumItemFinanciavel.SERVICO_GEOLOCALIZACAO.getValue(), valorCobradoCliente,BigDecimal.ZERO, false, false);
                        dadosCobrancaServicoGeolocalizacao.setItemFinanciavelServicoGeolocalizacao(itemFinanciavelGeolocalizacao);
                    }

                    dadosCobrancaServicoGeolocalizacao.setQtdMeses(parametrizacaoEncontrada.getQtdMeses());
                    dadosCobrancaServicoGeolocalizacao.setQtdMesesSubsidioBTB(qtdMesesSubsidioBTB);
                    dadosCobrancaServicoGeolocalizacao.setQtdMesesSubsidioTDB(qtdMesesSubsidioTDB);
                    dadosCobrancaServicoGeolocalizacao.setQtdMesesSubsidioSeguradora(qtdMesesSubsidioSeguradora);
                    dadosCobrancaServicoGeolocalizacao.setQtdMesesCobradoDoCliente(qtdMesesCobradoDoCliente);
                    dadosCobrancaServicoGeolocalizacao.setValorCobradoCliente(valorCobradoCliente);
                    dadosCobrancaServicoGeolocalizacao.setValorRepasseBTB(valorRepasseBTB);

                    return dadosCobrancaServicoGeolocalizacao;
                }
            }
        }

        return null;
    }
}