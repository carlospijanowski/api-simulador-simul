package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.repository.RedisRepository;
import br.com.bancotoyota.services.simulador.services.MotorTaxaPlanoService;
import br.com.bancotoyota.services.simulador.services.PlanoService;
import br.com.bancotoyota.services.simulador.services.SimulacaoServices;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SimulacaoServicesImpl implements SimulacaoServices {

    private static final String KEY_IOF = "iof:%s";

    @Autowired
    private FeatureToggleConfig featureToggleConfig;

    private RedisRepository redisRepository;

    private PlanoService planoService;

    private MotorTaxaPlanoService motorTaxaPlanoService;

    private static final TaxasIOF IOF_ZERO = new TaxasIOF("365.000000", "0.000000", "0.000000");

    @Autowired
    public SimulacaoServicesImpl(RedisRepository redisRepository, PlanoService planoService, MotorTaxaPlanoService motorTaxaPlanoService, FeatureToggleConfig featureToggleConfig) {
        this.redisRepository = redisRepository;
        this.planoService = planoService;
        this.motorTaxaPlanoService = motorTaxaPlanoService;
        this.featureToggleConfig = featureToggleConfig;
    }

    private static boolean isNotTaxaZero(Prazo p) {
        return p.getTaxaBanco().compareTo(BigDecimal.ZERO) != 0;
    }

    @Override
    public List<Prazo> getParcelas(Integer planoId, BigDecimal percentualEntrada, Collection<Integer> meses,
                                   BigDecimal taxaMes, Integer prazoDesejado) {

        if (planoId != -1) {
            List<Prazo> prazos = new ArrayList<>();
            if (BooleanUtils.isTrue(featureToggleConfig.getBuscaApiMotorTaxaEnabled())) {
                prazos = motorTaxaPlanoService.findPrazos(planoId, meses);
            } else {
                prazos = planoService.findPrazos(planoId, meses);
            }


            if (meses != null && !meses.isEmpty() && prazos.isEmpty()) {
                throw new BusinessValidationException(
                        EnumErroValidacao.ERRO_PLANO_NAO_PODE_SER_USADO_COM_DETERMINADA_QUANTIDADE_PARCELAS.getKey(),
                        EnumErroValidacao.ERRO_PLANO_NAO_PODE_SER_USADO_COM_DETERMINADA_QUANTIDADE_PARCELAS.getValue());
            }
            if (percentualEntrada != null) {
                prazos = filtrarParcelas(prazos, percentualEntrada);
                if (prazos.isEmpty()) {
                    throw new BusinessValidationException("percentual-entrada", percentualEntrada.toString(),
                            EnumErroValidacao.ERRO_PERCENTUAL_ENTRADA_FORA_LIMITE_PLANO.getValue(),
                            EnumErroValidacao.ERRO_PERCENTUAL_ENTRADA_FORA_LIMITE_PLANO.getKey());
                }
            }
            if (taxaMes != null) {
                prazos.forEach(p -> p.setTaxaBanco(taxaMes));
            }
            if (prazoDesejado != null && prazos.size() > 1) {
                Prazo prazo = prazos.stream().filter(p -> p.getNumeroDeParcelas() == prazoDesejado).findFirst().orElse(null);
                if (prazo != null)
                    prazos = prazos.stream().filter(p -> p.getTaxaBanco().compareTo(prazo.getTaxaBanco()) <= 0).collect(Collectors.toList());
            }

            return prazos;
        } else {
            validarMesesTaxaMes(meses, taxaMes);
            return meses.stream().map(i -> new Prazo(i, taxaMes, BigDecimal.ZERO, NumberUtils.CEM, BigDecimal.ZERO,
                    NumberUtils.CEM,null)).collect(Collectors.toList());
        }
    }


    private void validarMesesTaxaMes(Collection<Integer> meses, BigDecimal taxaMes) {
        if (meses == null) {
            throw new BusinessValidationException(Constants.PARCELAS, null,
                    "quando o id do plano não é passado (-1) é necessário especificar o número de parcelas");
        }
        if (taxaMes == null) {
            throw new BusinessValidationException("taxa-mes", null,
                    "quando o id do plano não é passado (-1) é necessário especificar a taxa mês");
        }
    }

    private List<Prazo> filtrarParcelas(List<Prazo> prazos, BigDecimal percentualEntrada) {
        prazos = prazos.stream().filter(SimulacaoServicesImpl::isNotTaxaZero).collect(Collectors.toList());
        prazos = prazos.stream().filter(p ->
                p.getPercentualMaxEntrada().compareTo(BigDecimal.ZERO) == 0 ||
                        (percentualEntrada.compareTo(p.getPercentualMaxEntrada()) <= 0) &&
                                (percentualEntrada.compareTo(p.getPercentualMinEntrada()) >= 0)
        ).collect(Collectors.toList());
        return prazos;
    }

    @Override
    public TaxasIOF getTaxasIOF(TipoPessoa tipoPessoa, String isentaIof) {
        if (isentaIof != null && isentaIof.equalsIgnoreCase("S")) {
            return IOF_ZERO;
        }

        String tipo = null;
        if (tipoPessoa == TipoPessoa.PESSOA_JURIDICA) {
            tipo = "pj";
        }
        if (tipoPessoa == TipoPessoa.PESSOA_FISICA) {
            tipo = "pf";
        }
        if (tipo == null) throw new HttpMessageConversionException("tipo-pessoa inválido");
        String key = String.format(KEY_IOF, tipo);
        return redisRepository.findTaxasIOF(key);
    }

    @Override
    public Subsidio getSubsidio(Integer planoId) {
        if (BooleanUtils.isTrue(featureToggleConfig.getBuscaApiMotorTaxaEnabled())) {
            Subsidio subsidio = motorTaxaPlanoService.findSubsidio(planoId);
            return subsidio;
        } else {
            return planoService.findSubsidio(planoId);
        }
    }

    @Override
    public void validaRequestSimulacao(SimulacaoParcResidualRequest request) {
        if (ObjectUtils.isEmpty(request.getParcelas())) {
            throw new BusinessValidationException(Constants.PARCELAS, "É necessário especificar o número de parcelas");
        }

        if (request.getParcelas().length > 1) {
            throw new BusinessValidationException(Constants.PARCELAS, "É permitido especificar somente um número de parcelas");
        }
    }
}
