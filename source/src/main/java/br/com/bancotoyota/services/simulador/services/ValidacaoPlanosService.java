package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.response.PlanoMotorTaxa;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.entities.Plano;
import br.com.bancotoyota.services.simulador.entities.Prazo;
import br.com.bancotoyota.services.simulador.entities.Retorno;
import br.com.bancotoyota.services.simulador.entities.Subsidio;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ValidacaoPlanosService {

    @Autowired
    private ObjectMapper mapper;

    @Autowired(required = false)
    @Setter(AccessLevel.PROTECTED)
    private PlanoService planoService;

    @Autowired(required = false)
    @Setter(AccessLevel.PROTECTED)
    private MotorTaxaPlanoService motorTaxaPlanoService;


    /**
     * Faz a validação dos planos do Motor de Taxas para ver se estão com os mesmos valores que no PlanosV2
     */
    public Map<String, List<String>> validaPlanosMotorTaxas(Integer planoId, String cnpjOrigemNegocio, Boolean mostrarDiferencas, Boolean retornarJson){
        List<PlanoMotorTaxa> todosPlanos = new ArrayList<>();

        if (planoId != null) {
            todosPlanos.add(motorTaxaPlanoService.getPlanoPorId(planoId));
        }else{
            todosPlanos = motorTaxaPlanoService.getPlanos();
        }

        Map<String,List<String>> diffs = new HashMap<>();

        for (PlanoMotorTaxa planoMotorTaxa : todosPlanos) {

            List<String> diferencas = new ArrayList<>();

            Plano planoMotor = motorTaxaPlanoService.convertPlanoFromMotorTaxa(planoMotorTaxa);
            List<Prazo> prazosMotor = motorTaxaPlanoService.findPrazos(planoMotorTaxa.getPlanoId(), null);
            List<Retorno> codigosSimulacaoVendedorMotor = motorTaxaPlanoService.getRetornos(planoMotorTaxa.getPlanoId().toString(), cnpjOrigemNegocio, "VENDEDOR", null);
            List<Retorno> codigosSimulacaoMotor = motorTaxaPlanoService.getRetornos(planoMotorTaxa.getPlanoId().toString(), cnpjOrigemNegocio, "COORDENADOR", null);

            try {

                Plano planoV2 = planoService.getPlanoById(planoMotorTaxa.getPlanoId());
                List<Prazo> prazosPlanoV2 = planoService.findPrazos(planoMotorTaxa.getPlanoId(), null);
                List<Retorno> codigosSimulacaoVendedorPlanosV2 = planoService.getRetornos(planoMotorTaxa.getPlanoId().toString(), cnpjOrigemNegocio, "VENDEDOR", "");
                List<Retorno> codigosSimulacaoPlanosV2 = planoService.getRetornos(planoMotorTaxa.getPlanoId().toString(), cnpjOrigemNegocio, "COORDENADOR", "");
                Subsidio subsidioMotor = motorTaxaPlanoService.findSubsidio(planoMotorTaxa.getPlanoId());
                Subsidio subsidioPlanosV2 = planoService.findSubsidio(planoMotorTaxa.getPlanoId());

                ObjectMapper mapper = new ObjectMapper();


                Boolean encontrouDiferencas = false;
                if (!assertEqualPlano(planoV2,planoMotor)) {
                    if (BooleanUtils.isTrue(mostrarDiferencas)) {
                        String jsonPlanoMotor = mapper.writeValueAsString(planoMotor);
                        String jsonPlanoV2 = mapper.writeValueAsString(planoV2);
                        diferencas.add("Plano Motor: " + jsonPlanoMotor);
                        diferencas.add("Plano PlanosV2: " + jsonPlanoV2);
                    }else{
                        diferencas.add("Plano NOK");
                    }
                    encontrouDiferencas = true;
                }else if (BooleanUtils.isTrue(retornarJson)) {
                    String jsonPlanoMotor = mapper.writeValueAsString(planoMotor);
                    String jsonPlanoV2 = mapper.writeValueAsString(planoV2);
                    diferencas.add("Plano Motor: " + jsonPlanoMotor);
                    diferencas.add("Plano PlanosV2: " + jsonPlanoV2);
                }
                if (!assertEqualSubsidio(subsidioPlanosV2, subsidioMotor)) {
                    if (BooleanUtils.isTrue(mostrarDiferencas)) {
                        String jsonSubsidioMotor = mapper.writeValueAsString(subsidioMotor);
                        String jsonSubsidioPlanoV2 = mapper.writeValueAsString(subsidioPlanosV2);
                        diferencas.add("Subsidio Motor: " + jsonSubsidioMotor);
                        diferencas.add("Subsidio PlanosV2: " + jsonSubsidioPlanoV2);
                    }else{
                        diferencas.add("Subsidio NOK");
                    }
                    encontrouDiferencas = true;
                }else if (BooleanUtils.isTrue(retornarJson)) {
                    String jsonSubsidioMotor = mapper.writeValueAsString(subsidioMotor);
                    String jsonSubsidioPlanoV2 = mapper.writeValueAsString(subsidioPlanosV2);
                    diferencas.add("Subsidio Motor: " + jsonSubsidioMotor);
                    diferencas.add("Subsidio PlanosV2: " + jsonSubsidioPlanoV2);
                }


                if (!assertEqualPrazos(prazosPlanoV2, prazosMotor)) {
                    if (BooleanUtils.isTrue(mostrarDiferencas)) {
                        String jsonPrazosMotor = mapper.writeValueAsString(prazosMotor);
                        String jsonPrazosPlanoV2 = mapper.writeValueAsString(prazosPlanoV2);
                        diferencas.add("Prazos Motor: " + jsonPrazosMotor);
                        diferencas.add("Prazos PlanosV2: " + jsonPrazosPlanoV2);
                    } else {
                        diferencas.add("Prazos NOK");
                    }
                    encontrouDiferencas = true;
                }else if (BooleanUtils.isTrue(retornarJson)) {
                    String jsonPrazosMotor = mapper.writeValueAsString(prazosMotor);
                    String jsonPrazosPlanoV2 = mapper.writeValueAsString(prazosPlanoV2);
                    diferencas.add("Prazos Motor: " + jsonPrazosMotor);
                    diferencas.add("Prazos PlanosV2: " + jsonPrazosPlanoV2);
                }

                if (!assertEqualCodigosSimulacao(codigosSimulacaoVendedorPlanosV2, codigosSimulacaoVendedorMotor)) {
                    if (BooleanUtils.isTrue(mostrarDiferencas)) {
                        String jsonCodigoSimulacaoVendedorMotor = mapper.writeValueAsString(codigosSimulacaoVendedorMotor);
                        String jsonCodigoSimulacaoVendedorPlanoV2 = mapper.writeValueAsString(codigosSimulacaoVendedorPlanosV2);
                        diferencas.add("CodigosSimulacaoVendedor Motor: " + jsonCodigoSimulacaoVendedorMotor);
                        diferencas.add("CodigosSimulacaoVendedor PlanosV2: " + jsonCodigoSimulacaoVendedorPlanoV2);
                    } else {
                        diferencas.add("CodigosSimulacaoVendedor NOK");
                    }
                    encontrouDiferencas = true;
                }else if (BooleanUtils.isTrue(retornarJson)) {
                    String jsonCodigoSimulacaoVendedorMotor = mapper.writeValueAsString(codigosSimulacaoVendedorMotor);
                    String jsonCodigoSimulacaoVendedorPlanoV2 = mapper.writeValueAsString(codigosSimulacaoVendedorPlanosV2);
                    diferencas.add("CodigosSimulacaoVendedor Motor: " + jsonCodigoSimulacaoVendedorMotor);
                    diferencas.add("CodigosSimulacaoVendedor PlanosV2: " + jsonCodigoSimulacaoVendedorPlanoV2);
                }


                if (!assertEqualCodigosSimulacao(codigosSimulacaoPlanosV2, codigosSimulacaoMotor)) {
                    if (BooleanUtils.isTrue(mostrarDiferencas)) {
                        String jsonCodigosSimulacaoMotor = mapper.writeValueAsString(codigosSimulacaoMotor);
                        String jsonCodigosSimulacaoPlanosV2 = mapper.writeValueAsString(codigosSimulacaoPlanosV2);
                        diferencas.add("CodigosSimulacao Motor: " + jsonCodigosSimulacaoMotor);
                        diferencas.add("CodigosSimulacao PlanosV2: " + jsonCodigosSimulacaoPlanosV2);

                    } else {
                        diferencas.add("CodigosSimulacao NOK");
                    }
                    encontrouDiferencas = true;
                }else if (BooleanUtils.isTrue(retornarJson)) {
                    String jsonCodigosSimulacaoMotor = mapper.writeValueAsString(codigosSimulacaoMotor);
                    String jsonCodigosSimulacaoPlanosV2 = mapper.writeValueAsString(codigosSimulacaoPlanosV2);
                    diferencas.add("CodigosSimulacao Motor: " + jsonCodigosSimulacaoMotor);
                    diferencas.add("CodigosSimulacao PlanosV2: " + jsonCodigosSimulacaoPlanosV2);
                }

                if (!diferencas.isEmpty()) {
                    if (BooleanUtils.isTrue(encontrouDiferencas)) {
                        diffs.put(planoMotor.getCodigoPlano() + " NOK", diferencas);
                    }else {
                        diffs.put(planoMotor.getCodigoPlano() + " OK", diferencas);
                    }
                }

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                if (e.getLocalizedMessage() != null) {
                    diferencas.add(e.getLocalizedMessage());
                }else{
                    diferencas.add(e.getStackTrace().toString());
                }
                diffs.put(planoMotor.getCodigoPlano() + " Erro ao chamar api PlanosV2 ", diferencas);
            }
        }
        return diffs;
    }

    private Boolean assertEqualPlano(Plano planoExpected, Plano plano){
        if ((planoExpected != null && planoExpected.getCodigoPlano() != null) && (plano != null && plano.getCodigoPlano() != null)) {
            if (ObjectUtils.equals( plano.getCodigoModalidade(), plano.getCodigoModalidade() ) &&
            ObjectUtils.equals( plano.getCodigoPlano(), plano.getCodigoPlano() ) &&
            ObjectUtils.equals( plano.getCodigoBasePlano(), plano.getCodigoBasePlano() ) &&
            ObjectUtils.equals( plano.getDescricao(), plano.getDescricao() ) &&
            ObjectUtils.equals( plano.getDataInicialVigencia(), plano.getDataInicialVigencia() ) &&
            ObjectUtils.equals( plano.getParcelaMinimaEntrada(), plano.getParcelaMinimaEntrada() ) &&
            ObjectUtils.equals( plano.getParcelaMaximaEntrada(), plano.getParcelaMaximaEntrada() ) &&
            ObjectUtils.equals( plano.getParcelaMinimaBalao(), plano.getParcelaMinimaBalao() ) &&
            ObjectUtils.equals( plano.getParcelaMaximaBalao(), plano.getParcelaMaximaBalao() ) &&
            ObjectUtils.equals( plano.getQuantidadeMinimaCarencia(), plano.getQuantidadeMinimaCarencia() ) &&
            ObjectUtils.equals( plano.getQuantidadeMaximaCarencia(), plano.getQuantidadeMaximaCarencia() ) &&
            ObjectUtils.equals( plano.getTipoPlano(), plano.getTipoPlano() ) &&
            ObjectUtils.equals( plano.getPlanoSubsidiado(), plano.getPlanoSubsidiado() ) &&
            ObjectUtils.equals( plano.getTipoSubsidio(), plano.getTipoSubsidio() ) &&
            ObjectUtils.equals( plano.getPrimeiroVencimentoFixo(), plano.getPrimeiroVencimentoFixo() ) &&
            ObjectUtils.equals( plano.getIndicadorFaixaAno(), plano.getIndicadorFaixaAno() ) &&
            ObjectUtils.equals( plano.getPrazoValidade(), plano.getPrazoValidade() ) &&
            ObjectUtils.equals( plano.getIdModalidade(), plano.getIdModalidade() ) &&
            ObjectUtils.equals( plano.getIsentaIof(), plano.getIsentaIof() ) &&
            ObjectUtils.equals( plano.getPeriodicidade(), plano.getPeriodicidade() ) &&
            ObjectUtils.equals( plano.getPeriodicidadeNumero(), plano.getPeriodicidadeNumero() ) &&
            ObjectUtils.equals( plano.getPermiteBalao(), plano.getPermiteBalao() ) &&
            ObjectUtils.equals( plano.getControleBalao(), plano.getControleBalao() ) ){
                //Não está validando o uso plano, anoModeloInicio e anoModeloFinal pois não encontrei nenhuma utilização das variáveis
                return true;
            }else{
                return false;
            }
        }else if ((planoExpected != null && planoExpected.getCodigoPlano() != null) ^ (plano != null && plano.getCodigoPlano() != null)){
            return false;
        }
        return true;
    }

    private Boolean assertEqualPrazos(List<Prazo> prazosExpected, List<Prazo> prazos){
        if ((prazosExpected != null && !prazosExpected.isEmpty()) && (prazos != null && !prazos.isEmpty())) {
            Boolean todosIguais = true;
            for (Prazo prazoExpected : prazosExpected) {
                Boolean igual = false;
                for (Prazo prazo : prazos) {
                    if (ObjectUtils.equals(prazo.getNumeroDeParcelas(), prazo.getNumeroDeParcelas()) &&
                    ObjectUtils.equals(prazo.getTaxaBanco(), prazo.getTaxaBanco()) &&
                    ObjectUtils.equals(prazo.getPercentualMinBalao(), prazo.getPercentualMinBalao()) &&
                    ObjectUtils.equals(prazo.getPercentualMaxBalao(), prazo.getPercentualMaxBalao()) &&
                    ObjectUtils.equals(prazo.getPercentualMaxEntrada(), prazo.getPercentualMaxEntrada()) &&
                    ObjectUtils.equals(prazo.getPercentualMinEntrada(), prazo.getPercentualMinEntrada())){
                        igual = true;
                        break;
                    }
                }
                if (BooleanUtils.isFalse(igual)) {
                    todosIguais = false;
                }
            }
            return todosIguais;
        }else if ((prazosExpected != null && !prazosExpected.isEmpty()) ^ (prazos != null && !prazos.isEmpty())){
            return false;
        }
        return true;
    }

    private Boolean assertEqualSubsidio(Subsidio subsidioExpected, Subsidio subsidio){
            if (subsidioExpected != null && subsidioExpected.getTipoSubsidio() != null &&
                subsidio != null && subsidio.getTipoSubsidio() != null) {
                if (ObjectUtils.equals(subsidioExpected.getTipoSubsidio(), subsidio.getTipoSubsidio()) &&
                ObjectUtils.equals(subsidio.getTaxaBanco(),subsidioExpected.getTaxaBanco()) &&
                ObjectUtils.equals(subsidio.getFixoPercentualMontadora(),subsidioExpected.getFixoPercentualMontadora()) &&
                ObjectUtils.equals(subsidio.getFixoPercentualRevendedora(),subsidioExpected.getFixoPercentualRevendedora()) &&
                ObjectUtils.equals(subsidio.getTipoDistribuicao(),subsidioExpected.getTipoDistribuicao()) &&
                ObjectUtils.equals(subsidio.getVpPercentualDistrMarca(),subsidioExpected.getVpPercentualDistrMarca()) &&
                ObjectUtils.equals(subsidio.getVpValorMaxMarca(),subsidioExpected.getVpValorMaxMarca())) {

                    if (subsidioExpected.getFaixasValor() != null && !subsidioExpected.getFaixasValor().isEmpty() &&
                            subsidio.getFaixasValor() != null && !subsidio.getFaixasValor().isEmpty()) {
                        Boolean todosIguais = true;
                        for (Subsidio.FaixaValor faixaValorExpected : subsidio.getFaixasValor()) {
                            Boolean igual = false;
                            for (Subsidio.FaixaValor faixaValor : subsidio.getFaixasValor()) {
                                if ( ObjectUtils.equals(faixaValorExpected.getValorInicial(), faixaValor.getValorInicial()) &&
                                        ObjectUtils.equals(faixaValorExpected.getValorFinal(), faixaValor.getValorFinal()) &&
                                        ObjectUtils.equals(faixaValorExpected.getResponsabilidade(), faixaValor.getResponsabilidade()) &&
                                        ObjectUtils.equals(faixaValorExpected.getSequencial(), faixaValor.getSequencial()) ) {
                                    igual = true;
                                    break;
                                }
                            }
                            if (BooleanUtils.isFalse(igual)) {
                                todosIguais = false;
                            }
                        }
                        return todosIguais;
                    }else if ((subsidioExpected.getFaixasValor() != null && !subsidioExpected.getFaixasValor().isEmpty()) ^
                            (subsidio.getFaixasValor() != null && !subsidio.getFaixasValor().isEmpty())){
                        return false;
                    }
                    return true;
                }else{
                    return false;
                }
        }else if ((subsidioExpected != null && subsidioExpected.getTipoSubsidio() != null) ^
                    (subsidio != null && subsidio.getTipoSubsidio() != null)){
            return false;
        }
        return true;
    }

    private Boolean assertEqualCodigosSimulacao(List<Retorno> codigosRetornoExpected, List<Retorno> codigosRetorno){
        if ( (codigosRetornoExpected != null && !codigosRetornoExpected.isEmpty()) && (codigosRetorno != null && !codigosRetorno.isEmpty()) ) {
                Boolean todosIguais = true;
                for (Retorno retornoExpected: codigosRetornoExpected) {
                    Boolean igual = false;
                    for (Retorno retorno: codigosRetorno) {
                        if ( ObjectUtils.equals(retornoExpected.getCodigoRetorno(),retorno.getCodigoRetorno()) &&
                             ObjectUtils.equals(retornoExpected.getCodigo(),retorno.getCodigo()) &&
                             ObjectUtils.equals(retornoExpected.getDescricao(),retorno.getDescricao())) {
                            igual = true;
                            break;
                        }
                    }
                    if (BooleanUtils.isFalse(igual)) {
                        todosIguais = false;
                    }
                }
                return todosIguais;
        }else if ( (codigosRetornoExpected != null && !codigosRetornoExpected.isEmpty()) ^ (codigosRetorno != null && !codigosRetorno.isEmpty()) ){
            return false;
        }
        return true;
    }
}
