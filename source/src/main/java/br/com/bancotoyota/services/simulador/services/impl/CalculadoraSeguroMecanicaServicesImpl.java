package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.ParametrosSeguroMecanica;
import br.com.bancotoyota.services.simulador.beans.response.CalculoSeguroMecanicaResponse;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.CalculadoraSeguroMecanicaServices;
import br.com.bancotoyota.services.simulador.services.SeguroGarantiaService;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Slf4j
public class CalculadoraSeguroMecanicaServicesImpl implements CalculadoraSeguroMecanicaServices {

    @Autowired
    private SeguroGarantiaService seguroGarantiaService;

    @Override
    public CalculoSeguroMecanicaResponse calcular(ParametrosSeguroMecanica parametrosSeguroMecanica, Integer prazo) {
        try {
            log.info("Iniciando cálculo do seguro garantia mecanica para os parâmetros: {} e prazo: {}", parametrosSeguroMecanica, prazo);
            ComboSeguroGarantiaResponse comboSeguroGarantiaResponse = obterCotacoes(parametrosSeguroMecanica);
            List<SeguroMecanica> listaSeguroMecanica = null;
            SeguroMecanica seguroSelecionado = null;
            if (comboSeguroGarantiaResponse.getCombo() != null) {
                String cpnjSeguradora = comboSeguroGarantiaResponse.getCombo().getCnpjSeguradora();
                String quilometragem = parametrosSeguroMecanica.getQuilometragem();
                log.debug("Cotaçoes de seguro garantia mecanica encontrado. Ordenando por prazo e valor.");
                List<ItemSeguroGarantia> cotacoesOrdenadas = ordenaCotacoesSeguroPorPrazoEValor(comboSeguroGarantiaResponse);
                listaSeguroMecanica = gerarListaSeguroGarantia(cotacoesOrdenadas, prazo, cpnjSeguradora, quilometragem);
                seguroSelecionado = selecionarSeguro(parametrosSeguroMecanica, cotacoesOrdenadas, prazo, cpnjSeguradora);
            } else {
                log.warn("Nenhuma cotação de seguro garantia mecanica encontrado para os parâmetros fornecidos.");
            }
            log.info("Cálculo do eguro garantia mecanica concluído. Retornando resposta.");
            return new CalculoSeguroMecanicaResponse(listaSeguroMecanica, seguroSelecionado, new ArrayList<>());
        } catch (BusinessValidationException e) {
            List<Erro> erros = new ArrayList<>();
            erros.add(new Erro(e.getMessage()));
            return new CalculoSeguroMecanicaResponse(new ArrayList<>(), null, erros);
        } catch (Exception e) {
            return null;
        }
    }

    private ComboSeguroGarantiaResponse obterCotacoes(ParametrosSeguroMecanica parametros) {
        log.debug("Obtendo cotações para o seguro com os parâmetros: {}", parametros);
        SeguroGarantiaRequest request = criarSeguroGarantiaRequest(parametros);
        ComboSeguroGarantiaResponse response = seguroGarantiaService.getValor(parametros.getCnpjOrigemNegocio(), request);
        log.debug("Cotações obtidas com sucesso: {}", response);
        return response;
    }

    private SeguroGarantiaRequest criarSeguroGarantiaRequest(ParametrosSeguroMecanica parametros) {
        log.debug("Criando requisição de seguro para parâmetros: {}", parametros);
        SeguroGarantiaRequest request = new SeguroGarantiaRequest();
        request.setAnoFabricacao(parametros.getAnoFabricacao());
        request.setDataInicioGarantia(parametros.getDataInicioSeguroMecanica());
        request.setPossuiGarantia(parametros.getPossuiSeguroMecanica());
        request.setMarca(parametros.getMarca());
        request.setAnoModelo(parametros.getAnoModelo());
        request.setModelo(parametros.getModelo());
        request.setCodigoFipe(parametros.getCodigoFipe());
        request.setQuilometragem(parametros.getQuilometragem());
        log.debug("Requisição de seguro criada: {}", request);
        return request;
    }

    private List<ItemSeguroGarantia> ordenaCotacoesSeguroPorPrazoEValor(ComboSeguroGarantiaResponse combo) {
        log.debug("Ordenando cotações de seguro por prazo e valor.");
        List<ItemSeguroGarantia> itens = combo.getCombo().getItens();
        itens.sort(Comparator.comparingInt(ItemSeguroGarantia::getPrazoSeguro)
                .thenComparing(ItemSeguroGarantia::getValorPremioTotal));
        log.debug("Cotações ordenadas: {}", itens);
        return itens;
    }

    private BigDecimal obterMenorValorPremioTotal(List<ItemSeguroGarantia> itens) {
        BigDecimal menorValor = itens.stream()
                .map(ItemSeguroGarantia::getValorPremioTotal)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        log.debug("Menor valor de prêmio total encontrado: {}", menorValor);
        return menorValor;
    }

    private List<SeguroMecanica> gerarListaSeguroGarantia(List<ItemSeguroGarantia> cotacoes, Integer prazo, String cnpjSeguradora, String quilometragem) {
        log.debug("Gerando lista de seguros garantia para prazo: {}", prazo);
        List<SeguroMecanica> listaSeguroMecanica = new ArrayList<>();
        for (ItemSeguroGarantia cotacao : cotacoes) {
            listaSeguroMecanica.add(criarSeguroGarantia(cotacao, prazo, cnpjSeguradora, quilometragem));
        }
        log.debug("Lista de seguros garantia gerada: {}", listaSeguroMecanica);
        return listaSeguroMecanica;
    }

    private SeguroMecanica selecionarSeguro(ParametrosSeguroMecanica parametros, List<ItemSeguroGarantia> itens, Integer prazo, String cpnjSeguradora) {
        log.debug("Selecionando cotação de seguro garatia com base nos parâmetros: {}", parametros);
        Optional<ItemSeguroGarantia> itemSelecionado = itens.stream()
                .filter(item -> parametros.getSeguroMecanicaSelecionado() != null
                        && parametros.getSeguroMecanicaSelecionado().getId().equals(item.getId())
                        && parametros.getSeguroMecanicaSelecionado().getPrazo().equals(item.getPrazoSeguro()))
                .findFirst();
        if (itemSelecionado.isPresent()) {
            log.debug("Seguro selecionado: {}", itemSelecionado.get());
        } else {
            log.warn("Nenhuma cotação de seguro garatia correspondente foi encontrado.");
        }
        return itemSelecionado
                .map(item -> criarSeguroGarantia(item, prazo, cpnjSeguradora, parametros.getQuilometragem()))
                .orElse(new SeguroMecanica());
    }

    private SeguroMecanica criarSeguroGarantia(ItemSeguroGarantia item, Integer prazo, String cnpjSeguradora, String quilometragem) {
        log.debug("Criando seguro garantia para o item: {} com prazo: {}", item, prazo);
        SeguroMecanica seguro = new SeguroMecanica();
        seguro.setPrazo(item.getPrazoSeguro());
        seguro.setId(item.getId());
        seguro.setQuilometragem(quilometragem);
        seguro.setDescricao(item.getDescricao());
        seguro.setCnpjSeguradora(cnpjSeguradora);
        seguro.setDataInicioSeguroMecanica(item.getInicioVigencia());
        seguro.setValorTotal(arredondarParaDuasCasasDecimais(item.getValorPremioTotal()));
        seguro.setTipo(item.getTipo());
        seguro.setValorParcela(arredondarParaDuasCasasDecimais(
                item.getValorPremioTotal().divide(BigDecimal.valueOf(prazo), RoundingMode.HALF_UP))
        );
        log.debug("Seguro garantia criado: {}", seguro);
        return seguro;
    }

    private BigDecimal arredondarParaDuasCasasDecimais(BigDecimal valor) {
        BigDecimal valorArredondado = valor.setScale(2, RoundingMode.HALF_UP);
        return valorArredondado;
    }
}