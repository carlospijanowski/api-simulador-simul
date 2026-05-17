package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.request.DadosPrestamista;
import br.com.bancotoyota.services.simulador.beans.request.FatorSimulacao;
import br.com.bancotoyota.services.simulador.beans.request.ParcelaIntermediaria;
import br.com.bancotoyota.services.simulador.beans.request.ValidacaoRequest;
import br.com.bancotoyota.services.simulador.beans.response.ParcelasIntermediarias;
import br.com.bancotoyota.services.simulador.beans.response.PlanoMotorTaxa;
import br.com.bancotoyota.services.simulador.beans.response.ValidacaoResponse;
import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.controller.completo.MocksParaTestesCompletos;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
public class SimuladorParcResidualControllerValidacaoTest {

    private SimuladorParcResidualController controller;

    private MocksParaTestesCompletos mocksParaTestesCompletos;
    @MockBean
    private CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices;

    @MockBean
    private CalculadoraServices calculadoraServices;

    @MockBean
    private SimulacaoServices simulacaoServices;

    @MockBean
    private PlanoService planoService;

    @MockBean
    private MotorTaxaPlanoService motorTaxaPlanoService;

    @MockBean
    private DataCarencia dataCarencia;

    @MockBean
    private ObjectMapper mapper;

    @MockBean
    private ClientService clientService;

    private ParametrosDeSeguroPrestamistaService parametrosDeSeguroPrestamistaService;

    @MockBean
    private SeguroPrestamistaPlusServiceImpl prestamistaService;

    private FeatureToggleConfig featureToggleConfig = new FeatureToggleConfig(Boolean.TRUE, Boolean.FALSE);

    @MockBean
    private FatorSimulacao fatorSimulacao;

    @MockBean
    private CalculadoraSeguroFranquiaServices seguroFranquiaServices;

    @Before
    public void setup() {
        mocksParaTestesCompletos = new MocksParaTestesCompletos();
        mocksParaTestesCompletos.planoService = planoService;
        mocksParaTestesCompletos.motorTaxaPlanoService = motorTaxaPlanoService;
        parametrosDeSeguroPrestamistaService  =
                new ParametrosDeSeguroPrestamistaService(clientService);
        controller = new SimuladorParcResidualController(
                calculadoraServices, simulacaoServices, dataCarencia, new BigDecimal(10),
                parametrosDeSeguroPrestamistaService, false, prestamistaService, featureToggleConfig, seguroFranquiaServices, fatorSimulacao, calculadoraSeguroMecanicaServices);
        controller.setPlanoService(planoService);
        controller.setMotorTaxaPlanoService(motorTaxaPlanoService);
        when(clientService.getValoresComprometidos(anyString())).thenReturn(
                new ValorComprometidoSeguroPrestamista.ClienteComprometimentoSeguro(BigDecimal.ZERO, BigDecimal.ZERO));
        List<Prazo> list = new ArrayList<>();
        Prazo prazo = new Prazo(12, new BigDecimal("1.25"), new BigDecimal(10), new BigDecimal(50),
                new BigDecimal(10), new BigDecimal(50),null);
        list.add(prazo);
        when(simulacaoServices.getParcelas(any(), any(), any(), isNull(), isNull())).thenReturn(list);

        Plano plano = new Plano();
        plano.setPermiteBalao(true);
        ParcelasIntermediarias parcelaTeste = new ParcelasIntermediarias();
        parcelaTeste.setPermiteBalao(true);
        plano.setParcelasIntermediarias(parcelaTeste);
        when(planoService.getPlanoById(any())).thenReturn(plano);
        when(motorTaxaPlanoService.getPlanoPorId(any())).thenReturn(new PlanoMotorTaxa());
        when(motorTaxaPlanoService.convertPlanoFromMotorTaxa(any())).thenReturn(plano);

    }

    @Test
    public void validarSemSubsidio() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));

    	EnumTipoValidacao tipoValidacao = EnumTipoValidacao.VALIDACAO_COMPLETA;
        ValidacaoRequest request = criarValidacaoRequest(tipoValidacao);
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertTrue(response.getErros().isEmpty());
    }

    @Test
    public void validarSemSubsidioEPermiteBalaoFalse() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
    	Plano plano = new Plano();
        plano.setPermiteBalao(false);
        //Rubens
        if(featureToggleConfig.getBuscaApiMotorTaxaEnabled()) {
            when(motorTaxaPlanoService.convertPlanoFromMotorTaxa(motorTaxaPlanoService.getPlanoPorId(any()))).thenReturn(plano);
        } else {
            when(planoService.getPlanoById(any())).thenReturn(plano);
        }

    	EnumTipoValidacao tipoValidacao = EnumTipoValidacao.VALIDACAO_COMPLETA;
        ValidacaoRequest request = criarValidacaoRequest(tipoValidacao);
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertTrue(response.getErros().isEmpty());
    }

    @Test
    public void validarResidualInferior() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setValorResidual(BigDecimal.ONE);
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertEquals(1, response.getErros().size());
        String str = response.getErros().iterator().next().getMessage();
        assertEquals("percentual do residual inferior ao limite mínimo de 10", str);
    }

    @Test
    public void validarResidualSuperior() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setValorResidual(request.getValorBem().subtract(BigDecimal.TEN));
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertEquals(1, response.getErros().size());
        String str = response.getErros().iterator().next().getMessage();
        assertEquals("percentual do residual superior ao limite máximo de 50", str);
    }

    @Test
    public void validarSemSubsidioTaxaFora() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setTaxaMensal(new BigDecimal("1.26"));
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertEquals(1, response.getErros().size());
        String descricaoErro = response.getErros().iterator().next().getMessage();
        assertEquals("taxa mensal deve ser 1.25", descricaoErro.trim());
    }

    @Test
    public void validarSemSubsidioTaxaForaRetornoMaiorQueZero() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setTaxaMensal(new BigDecimal("1.26"));
        request.setRetorno("1");
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertTrue(response.getErros().isEmpty());
    }

    @Test
    public void validarSemDataNascimento() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.getDadosPrestamista().setDataNascimento(null);
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertTrue(response.getErros().isEmpty());
    }

    @Test
    public void validarSemDataNascimentoComTipoValidacaoOferta() {
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.PRE_VALIDACAO_OFERTA);
        request.getDadosPrestamista().setDataNascimento(null);
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertTrue(response.getErros().isEmpty());
    }

    @Test
    public void validarSemDataNascimentoComEnriquecimentoOk() {
        List<String> erros = validarSemDataNascimentoComEnriquecimento(new BigDecimal(55000), LocalDateTime.of(1990,1,1,0,0));
        assertEquals(2, erros.size());
        assertTrue(erros.contains("tipo de seguro prestamista deve ser SPF"));
        assertTrue(erros.contains("valor do seguro prestamista inválido"));
    }

    private List<String> validarSemDataNascimentoComEnriquecimento(BigDecimal valorSeguro, LocalDateTime dataNascimentoEnriquecida) {
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.FORMATO_DATA_BRASIL);
    	String dataFormatada = dataNascimentoEnriquecida.format(formatter);
    	DadosCliente dadosCliente = new DadosCliente(dataFormatada);
        when(clientService.getDadosCliente(anyString())).thenReturn(dadosCliente);

        List<Seguro> seguros = Arrays.asList(new Seguro(TipoDeSeguroPrestamista.SPF,
                new BigDecimal(2500), new BigDecimal(200000), new BigDecimal(400000),
                new BigDecimal("1.25"), 15, 50)
        );
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(seguros, seguros.get(0)));

        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setValorSeguroPrestamista(valorSeguro);
        request.getOrigemNegocio().setSvpDisponivel(true);
        request.getOrigemNegocio().setSpfDisponivel(true);
        request.getDadosPrestamista().setDataNascimento(null);
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        return response.getErros().stream().map(Erro::getMessage).collect(Collectors.toList());
    }

    @Test
    public void validarSemDataNascimentoComEnriquecimentoOkVelho() {
        List<String> erros = validarSemDataNascimentoComEnriquecimento(BigDecimal.ZERO, LocalDateTime.of(1890,1,1,0,0));
        assertTrue(erros.isEmpty());
    }

    @Test
    public void validarSemDataNascimentoComEnriquecimentoErroValor() {
        List<String> erros = validarSemDataNascimentoComEnriquecimento(BigDecimal.ONE, LocalDateTime.of(1890,1,1,0,0));
        assertEquals(1, erros.size());
        assertEquals("valor do seguro prestamista inválido", erros.iterator().next());
    }

    @Test
    public void validarPlanoNaoEncontrado() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        when(simulacaoServices.getParcelas(any(), any(), any(), isNull(), isNull())).thenThrow(new EntityNotFoundException(Prazo.class,"id"));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertEquals(1, response.getErros().size());
        assertEquals("plano não encontrado", response.getErros().iterator().next().getMessage());
    }

    @Test
    public void validarPrazoInvalido() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        when(simulacaoServices.getParcelas(any(), any(), any(), isNull(), isNull())).thenThrow(new BusinessValidationException("prazo invalido"));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertEquals(1, response.getErros().size());
        assertEquals("prazo invalido", response.getErros().iterator().next().getMessage());
    }

    /**
     * A validação é PJ quando não é passado o campo DadosPrestamista.
     */
    @Test
    public void validarPJ() {
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setDadosPrestamista(null);
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertEquals(0, response.getErros().size());
    }

    /**
     * A validação é PJ quando não é passado o campo DadosPrestamista.
     */
    @Test
    public void validarPJInvalido() {
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setDadosPrestamista(null);
        request.setTipoDeSeguroPrestamista(TipoDeSeguroPrestamista.SPF);
        request.setValorSeguroPrestamista(BigDecimal.ONE);
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertEquals(2, response.getErros().size());
        List<String> erros = response.getErros().stream().map(Erro::getMessage).collect(Collectors.toList());
        assertTrue(erros.contains("tipo de seguro prestamista deve ser NENHUM"));
        assertTrue(erros.contains("valor do seguro prestamista inválido"));
    }

    @Test
    public void validarComSubsidioFixo() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        when(simulacaoServices.getSubsidio(any())).thenReturn(new Subsidio(EnumTipoSubsidio.FIXO,
                new BigDecimal("0.25"), null, null, null, null, null, null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setTaxaSubsidio(new BigDecimal("0.25"));
        request.setTaxaMensal(request.getTaxaSubsidio());
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertTrue(response.getErros().isEmpty());
    }

    @Test
    public void validarComSubsidioFixoTaxaSubsidioMaiorQueTaxaBanco() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        when(simulacaoServices.getSubsidio(any())).thenReturn(new Subsidio(EnumTipoSubsidio.FIXO,
                new BigDecimal("1.35"), null, null, null, null, null, null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        // a taxa aplicada será a taxa do plano por ser menos que a taxa de subsídio
        request.setTaxaSubsidio(new BigDecimal("1.25"));
        request.setTaxaMensal(request.getTaxaSubsidio());
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertTrue(response.getErros().isEmpty());
    }

    @Test
    public void naoDeveValidarComSubsidioFixoTaxaSubsidioMaiorQueTaxaBancoFalha() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        when(simulacaoServices.getSubsidio(any())).thenReturn(new Subsidio(EnumTipoSubsidio.FIXO,
                new BigDecimal("1.35"), null, null, null, null, null, null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        // a taxa aplicada será a taxa do plano por ser menos que a taxa de subsídio
        request.setTaxaSubsidio(new BigDecimal("1.35"));
        request.setTaxaMensal(request.getTaxaSubsidio());
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertEquals(0, response.getErros().size());
    }

    @Test
    public void validarComSubsidioVariavel() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        when(simulacaoServices.getSubsidio(any())).thenReturn(new Subsidio(EnumTipoSubsidio.VARIAVEL,
                new BigDecimal("0.25"), null, null, null, null, null, null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setTaxaSubsidio(new BigDecimal("0.15"));
        request.setTaxaMensal(request.getTaxaSubsidio());
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertTrue(response.getErros().isEmpty());
    }

    @Test
    public void validarComSubsidioVariavelTaxaMesDiferente() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        when(simulacaoServices.getSubsidio(any())).thenReturn(new Subsidio(EnumTipoSubsidio.VARIAVEL,
                new BigDecimal("0.25"), null, null, null, null, null, null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setTaxaSubsidio(new BigDecimal("0.15"));
        request.setTaxaMensal(request.getTaxaSubsidio().add(new BigDecimal("0.01")));
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertEquals(1, response.getErros().size());
        String str = response.getErros().iterator().next().getMessage();
        assertEquals("para planos com subsídio a taxa ao mês deve ser igual a taxa do subsídio", str);
    }

    @Test
    public void validarComSubsidioVariavelSemTaxaSubsidio() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        when(simulacaoServices.getSubsidio(any())).thenReturn(new Subsidio(EnumTipoSubsidio.VARIAVEL,
                new BigDecimal("0.25"), null, null, null, null, null, null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertEquals(1, response.getErros().size());
        String str = response.getErros().iterator().next().getMessage();
        assertEquals("taxa de subsídio não informada para plano com subsídio", str);
    }

    @Test
    public void validarComSubsidioVariavelNegativo() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        when(simulacaoServices.getSubsidio(any())).thenReturn(new Subsidio(EnumTipoSubsidio.VARIAVEL,
                new BigDecimal("0.25"), null, null, null, null, null, null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setTaxaSubsidio(new BigDecimal("-0.15"));
        request.setTaxaMensal(request.getTaxaSubsidio());
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertEquals(1, response.getErros().size());
        String str = response.getErros().iterator().next().getMessage();
        assertEquals("o mínimo para a taxa de subsídio é zero", str);
    }

    @Test
    public void validarComSubsidioVariavelMaiorQueTaxaBanco() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        when(simulacaoServices.getSubsidio(any())).thenReturn(new Subsidio(EnumTipoSubsidio.VARIAVEL,
                new BigDecimal("0.25"), null, null, null,
                null, null, null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setTaxaSubsidio(new BigDecimal("2.15"));
        request.setTaxaMensal(request.getTaxaSubsidio());
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertEquals(1, response.getErros().size());
        String str = response.getErros().iterator().next().getMessage();
        assertEquals("o máximo para a taxa de subsídio é 1.25", str);
    }

    @Test
    public void validarComSubsidioValorMenorQueOAceitavel() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        when(simulacaoServices.getSubsidio(any())).thenReturn(new Subsidio(EnumTipoSubsidio.VARIAVEL,
                new BigDecimal("0.25"), null, null, null,
                null, null, null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setValorMaximoSubsidio(new BigDecimal(10));
        request.setValorSubsidio(new BigDecimal(1));

        request.setTaxaMensal(request.getTaxaSubsidio());
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertEquals(1, response.getErros().size());
        String str = response.getErros().iterator().next().getMessage();
        assertEquals("O valor do subsídio é de R$ 10 a R$ 10", str);
    }


    @Test
    public void validarComSubsidioOk() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        when(simulacaoServices.getSubsidio(any())).thenReturn(new Subsidio(EnumTipoSubsidio.VARIAVEL,
                new BigDecimal("10"), null, null, null,
                null, null, null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setValorMaximoSubsidio(new BigDecimal(10));
        request.setValorSubsidio(new BigDecimal(10));

        request.setTaxaMensal(request.getTaxaSubsidio());
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertEquals(0, response.getErros().size());
    }

    @Test
    public void validarEntradaMaisIntermediariasMaiorQueBem() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        when(simulacaoServices.getSubsidio(any())).thenReturn(new Subsidio(EnumTipoSubsidio.VARIAVEL,
                new BigDecimal("10"), null, null, null,
                null, null, null));
        when(motorTaxaPlanoService.getPlanoPorId(any())).thenReturn(new PlanoMotorTaxa());

        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setIntermediarias(new ArrayList<>());
        ParcelaIntermediaria parc = new ParcelaIntermediaria();
        parc.setValor(new BigDecimal(999999));
        parc.setVencimento(LocalDate.now());
        request.getIntermediarias().add(parc);
        request.setValorMaximoSubsidio(new BigDecimal(10));
        request.setValorSubsidio(new BigDecimal(10));

        request.setTaxaMensal(request.getTaxaSubsidio());
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertEquals(1, response.getErros().size());
    }


    @Test
    public void validarComSubsidioValorMaiorQueOAceitavel() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        when(simulacaoServices.getSubsidio(any())).thenReturn(new Subsidio(EnumTipoSubsidio.VARIAVEL,
                new BigDecimal("0.25"), null, null, null,
                null, null, null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setValorMaximoSubsidio(new BigDecimal(10));
        request.setValorSubsidio(new BigDecimal(100));

        request.setTaxaMensal(request.getTaxaSubsidio());
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertEquals(1, response.getErros().size());
        String str = response.getErros().iterator().next().getMessage();
        assertEquals("O valor do subsídio é de R$ 10 a R$ 10", str);
    }


    public static ValidacaoRequest criarValidacaoRequest(EnumTipoValidacao tipoValidacao) {

        OrigemNegocio origemNegocio = new OrigemNegocio(null, null, null, null, null, null, null,
                null, null, false, false, false, false, false, false);
        DadosPrestamista dadosPrestamista = new DadosPrestamista(null, null,
                LocalDate.of(1980, 1, 1), "50122818539", new BigDecimal(45000),
                new BigDecimal(1000), new BigDecimal(1500),true,true, null);
        return new ValidacaoRequest(Modalidade.CICLO_TOYOTA, origemNegocio, dadosPrestamista, TipoDeSeguroPrestamista.NENHUM, BigDecimal.ZERO,
                2834, 12, new BigDecimal(70000),
                new BigDecimal(25000), new BigDecimal(10000), null, "0", new BigDecimal("1.25"), null,null,null, tipoValidacao);
    }

    @Test
    public void testeDeValidacaoSemTaxaMensalSemSubsidio() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setTaxaMensal(null);
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertEquals(0, response.getErros().size());
    }

    @Test
    public void testeDeValidacaoSemTaxaMensalComSubsidio() {
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(new ArrayList<>(), null));
        when(simulacaoServices.getSubsidio(any())).thenReturn(new Subsidio(EnumTipoSubsidio.FIXO,
                new BigDecimal("0.25"), null, null, null,
                null, null, null));
        ValidacaoRequest request = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        request.setTaxaSubsidio(new BigDecimal("0.25"));
        request.setTaxaMensal(null);
        ResponseEntity<ValidacaoResponse> retorno = controller.validar(request, "token");
        assertNotNull(retorno);
        ValidacaoResponse response = retorno.getBody();
        assertNotNull(response);
        assertTrue(response.getErros().isEmpty());
    }
}
