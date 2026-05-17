package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcDesejadaRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoRequest;
import br.com.bancotoyota.services.simulador.entities.Carencia;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@RunWith(SpringRunner.class)
public class SimulacaoRequestTest {

    @Test
    public void testPercentualEntrada(){
        SimulacaoRequest r = getSimulacaoRequest();

        r.setValorBem(BigDecimal.valueOf(100));
        r.setValorEntrada(BigDecimal.TEN);

        BigDecimal percent = r.getPercentualEntrada();

        Assert.assertEquals(BigDecimal.valueOf(10.00000000).setScale(8, RoundingMode.DOWN), percent);
    }


    @Test
    public void testPercentualEntradaSemEntrada(){
        SimulacaoRequest r = getSimulacaoRequest();

        r.setValorEntrada(BigDecimal.ZERO);

        BigDecimal percent = r.getPercentualEntrada();

        Assert.assertEquals(BigDecimal.ZERO, percent);

    }

    private SimulacaoRequest getSimulacaoRequest(){
        return  new SimulacaoRequest() {
            @Override
            public @NotNull Carencia getCarenciaDoPlano() {
                return super.getCarenciaDoPlano();
            }
        };
    }

    @Test
    public void testaIdade() {
        SimulacaoRequest request = new SimulacaoParcDesejadaRequest();
        LocalDate now = LocalDate.now();
        request.setDataNascimento(LocalDate.of(now.getYear() - 40, now.getMonthValue(), now.getDayOfMonth()));
        int idade = request.getIdade();
        System.out.println(request.getDataNascimento());
        Assert.assertEquals(40, idade);

        request.setDataNascimento(request.getDataNascimento().minusMonths(11));
        System.out.println(request.getDataNascimento());
        Assert.assertEquals(40, request.getIdade().intValue());

        request.setDataNascimento(request.getDataNascimento().minusDays(27));
        System.out.println(request.getDataNascimento());
        Assert.assertEquals(40, request.getIdade().intValue());

        request.setDataNascimento(LocalDate.of(now.getYear() - 40, now.getMonthValue(), now.getDayOfMonth()));
        request.setDataNascimento(request.getDataNascimento().plusDays(1));
        Assert.assertEquals(39, request.getIdade().intValue());
    }
}
