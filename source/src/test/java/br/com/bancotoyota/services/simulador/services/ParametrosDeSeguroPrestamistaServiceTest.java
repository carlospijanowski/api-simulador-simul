package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.entities.OrigemNegocio;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ParametrosDeSeguroPrestamistaServiceTest {

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ParametrosDeSeguroPrestamistaService service;

    @Test
    public void testeSemCPF() {
        OrigemNegocio origemNegocio = new OrigemNegocio();
        origemNegocio.setSpfDisponivel(true);
        origemNegocio.setSvpDisponivel(true);
        origemNegocio.setSvpSeSpfRemovido(true);
        ParametrosDeSeguroPrestamista psp = new ParametrosDeSeguroPrestamista();
        psp.setValorOriginalTotalFinanciado(BigDecimal.TEN);
        psp.setSeguroPrestamistaEscolhido(TipoDeSeguroPrestamista.NENHUM);
        ParametrosDeSeguroPrestamista psp2 = service.getParametros(origemNegocio, null, "token", psp, null);
        validarComoClienteNovo(psp, psp2);
    }

    /**
     * A validação para quando o CPF não é passado é a mesma de quando o serviço deu erro.
     */
    private void validarComoClienteNovo(ParametrosDeSeguroPrestamista psp, ParametrosDeSeguroPrestamista psp2) {
        assertEquals(psp.getValorOriginalTotalFinanciado(), psp2.getValorOriginalTotalFinanciado());
        assertEquals(psp.getSeguroPrestamistaEscolhido(), psp2.getSeguroPrestamistaEscolhido());
        assertEquals(BigDecimal.ZERO, psp2.getSpfValorComprometido());
        assertEquals(BigDecimal.ZERO, psp2.getSvpValorComprometido());
    }

    @Test
    public void testeComErro() {
        when(clientService.getValoresComprometidos(anyString())).thenThrow(new ThirdPartyException("erro", null));
        testeComErro(null);
        testeComErro(new ArrayList<>());
    }

    private void testeComErro(List<Erro> erros) {
        OrigemNegocio origemNegocio = new OrigemNegocio();
        origemNegocio.setSpfDisponivel(true);
        origemNegocio.setSvpDisponivel(true);
        origemNegocio.setSvpSeSpfRemovido(true);
        ParametrosDeSeguroPrestamista psp = new ParametrosDeSeguroPrestamista();
        psp.setValorOriginalTotalFinanciado(BigDecimal.TEN);
        psp.setSeguroPrestamistaEscolhido(TipoDeSeguroPrestamista.NENHUM);
        ParametrosDeSeguroPrestamista psp2 = service.getParametros(origemNegocio, "cpf", "token", psp, erros);
        if (erros != null) {
            assertEquals(1, erros.size());
        }
        validarComoClienteNovo(psp, psp2);
    }

    @Test
    public void testeComErroDataNascimento() {
        when(clientService.getDadosCliente(anyString())).thenThrow(new ThirdPartyException("erro", null));
        List<Erro> erros = new ArrayList<>();
        service.getDataNascimento(null, "cpf", "token", erros);
        assertEquals(1, erros.size());
    }

    @Test
    public void testeRequestPropoComIdParametrosPrestamistaSetado() {
        OrigemNegocio origemNegocio = new OrigemNegocio();
        origemNegocio.setSpfDisponivel(true);
        origemNegocio.setSvpDisponivel(true);
        origemNegocio.setSvpSeSpfRemovido(true);
        ParametrosDeSeguroPrestamista psp = new ParametrosDeSeguroPrestamista();
        psp.setValorOriginalTotalFinanciado(BigDecimal.TEN);
        psp.setSeguroPrestamistaEscolhido(TipoDeSeguroPrestamista.NENHUM);
        psp.setIdSeguroEscolhido(Integer.valueOf("99"));
        psp.setSegurosPrestamistasExcluidos(new ArrayList<>());

        ParametrosDeSeguroPrestamista psp2 = service.getParametros(origemNegocio, null, "token", psp, null);
        assertEquals(Integer.valueOf("99"), psp2.getIdSeguroEscolhido());
        assertTrue(psp2.getSegurosPrestamistasExcluidos().isEmpty());
    }
}