package br.com.bancotoyota.services.simulador.services.impl;


import br.com.bancotoyota.services.simulador.beans.*;
import br.com.bancotoyota.services.simulador.beans.request.*;
import br.com.bancotoyota.services.simulador.config.*;
import br.com.bancotoyota.services.simulador.controller.*;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.exceptions.*;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.*;
import br.com.bancotoyota.services.simulador.utils.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import org.apache.commons.io.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;

import java.io.*;
import java.lang.reflect.*;
import java.math.*;
import java.time.*;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class CalculadoraParcelasResidualImplTest {

    @InjectMocks
    private CalculadoraParcelasResidualImpl calculadora;

    @Mock
    private LogService logService;
    @Mock
    private PlanoService planoService;
    @Mock
    private CalculadoraOfertas calculadoraOfertas;
    @Mock
    private MotorTaxaPlanoService motorTaxaPlanoService;
    @Mock
    private SimulacaoServices simulacaoServices;
    @Mock
    private CalculadoraSeguroFranquiaServices seguroFranquiaServices;
    @Mock
    private FeatureToggleConfig featureToggleConfig;
    @Mock
    private SimulacaoParcResidual simulacaoParcResidual;
    @Mock
    private CalculadoraServices calculadoraServices;
    @Mock
    private DataCarencia dataCarencia;
    @Mock
    private SeguroPrestamistaPlusServiceImpl prestamistaService;

    private Plano plano;
    ObjectMapper mapper = new ObjectMapper();
    private ResultadoParcelaResidual responseResultadoParcelaResidual;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        plano = new Plano("ciclo-toyota", "2838",
                "2738", "CICLO LXT>30 121", 2018, 9999,
                LocalDateTime.parse("2019-03-14 00:00:00.000", LocalDateTimeSerializer.DATE_TIME_FORMATTER),
                new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), new BigDecimal("30.000000"),
                new BigDecimal("100.000000"), new BigDecimal("0.00"), new BigDecimal("50.00"),
                10, 90, "CDC-TCM", false,
                null, false, "AI_E_0KM", 30, "12", "N", "MENSAL",
                1, true, false, EnumControleBalao.PERMITE_BALAO, null, false, null);
        mapper.findAndRegisterModules();
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        responseResultadoParcelaResidual = mapper.readValue(IOUtils.toString(getClass().getClassLoader().getResourceAsStream("desejada/simulacao-tipo-A-tipo-de-subsidio-iguais.json"), "UTF-8"), ResultadoParcelaResidual.class);

    }


    @Test
    void deveExecutarSimulacaoComSucesso() {
        Integer[] parcelas = {12, 24, 36};
        SimulacaoParcResidualRequest request = criarRequest(parcelas, 160850.00, Modalidade.CICLO_TOYOTA);
        when(planoService.getPlanoById(any())).thenReturn(plano);
        when(featureToggleConfig.getCicloIntermediariasEnabled()).thenReturn(false);
        TaxasIOF taxasIOF = new TaxasIOF("365.000000", "0.030000", "0.003800");
        when(simulacaoServices.getTaxasIOF(any(), any())).thenReturn(taxasIOF);
        List<Prazo> prazoList = new ArrayList<>();
        prazoList.add(new Prazo(12, new BigDecimal("1.308033"), new BigDecimal("20.00"),
                new BigDecimal("50.00"), new BigDecimal(20), new BigDecimal(50), null));
        prazoList.add(new Prazo(24, new BigDecimal("1.308033"), new BigDecimal("20.00"),
                new BigDecimal("50.00"), new BigDecimal(20), new BigDecimal(50), null));
        prazoList.add(new Prazo(36, new BigDecimal("1.308033"), new BigDecimal("20.00"),
                new BigDecimal("40.00"), new BigDecimal(20), new BigDecimal(50), null));
        BigDecimal limiteDeFinanciamento = new BigDecimal(90000);
        Seguro spf = new Seguro(TipoDeSeguroPrestamista.SPF, new BigDecimal(2500), limiteDeFinanciamento,
                new BigDecimal("450000.00"), new BigDecimal("0.029000"), 18, 65);
        Seguro svp = new Seguro(TipoDeSeguroPrestamista.SVP, BigDecimal.ZERO, limiteDeFinanciamento,
                new BigDecimal("990000.00"), new BigDecimal("0.012000"), 18, 65);

        List<Seguro> seguros = Arrays.asList(spf, svp);
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(seguros, seguros.get(1)));
        when(simulacaoServices.getParcelas(anyInt(), any(), any(), isNull(), isNull())).then(i -> prazoList);


        ResultadoParcelaResidual resultado = calculadora.fazerSimulacao(request, false, false, LocalDate.now(), false);

        assertNotNull(resultado);
    }

    @Test
    void deveLancarExcecaoQuandoEntradaMaisIntermediariasExcedeValorBem() {
        Integer[] parcelas = {12, 24, 36};
        SimulacaoParcResidualRequest request = criarRequest(parcelas, 40000, Modalidade.CICLO_TOYOTA);
        request.setValorParcelaResidual(BigDecimal.valueOf(15000));
        when(planoService.getPlanoById(any())).thenReturn(plano);
        when(featureToggleConfig.getCicloIntermediariasEnabled()).thenReturn(false);

        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> calculadora.criarSimulacao(request, false, new ArrayList<>(), false, LocalDate.now(), false));

        assertEquals("Valor da entrada muito alto. Entrada mais valor para a parcela residual/intermediárias não pode ser maior que o valor do bem.", ex.getMessage());
    }

    @Test
    void deveCarregarPrazosCorretamente() {
        Integer[] parcelas = {12, 24, 36};
        SimulacaoParcResidualRequest request = criarRequest(parcelas, 40000, Modalidade.CICLO_TOYOTA);

        List<Prazo> prazosMock = List.of(new Prazo(), new Prazo());
        when(simulacaoServices.getParcelas(any(), any(), any(), any(), any())).thenReturn(prazosMock);


        List<Prazo> prazos = calculadora.carregarPrazos(request);

        assertEquals(2, prazos.size());
    }


    @Test
    void deveLancarExcecaoQuandoValorResidualMaiorQueBemMenosEntrada() throws Exception {
        Integer[] parcelas = {12, 24, 36};
        SimulacaoParcResidualRequest request = criarRequest(parcelas, 40000, Modalidade.CICLO_TOYOTA);
        request.setValorEntrada(BigDecimal.valueOf(1000));
        request.setValorParcelaResidual(BigDecimal.valueOf(60000));
        request.setPercentuaisBalao(Collections.emptyList());

        var metodo = CalculadoraParcelasResidualImpl.class
                .getDeclaredMethod("validarValorResidualNoRangeBalao", SimulacaoParcResidualRequest.class);
        metodo.setAccessible(true);

        BusinessValidationException ex = assertThrows(
                BusinessValidationException.class,
                () -> {
                    try {
                        metodo.invoke(calculadora, request);
                    } catch (InvocationTargetException e) {
                        throw (Exception) e.getCause();
                    }
                }
        );
        assertEquals("Valor residual muito alto. Valor residual nao pode ultrapassar o valor do bem abatido da entrada.",
                ex.getMessage());
    }


    private SimulacaoParcResidualRequest criarRequest(Integer[] parcelas, double valorDoBem, Modalidade modalidade) {
        BigDecimal valor_uf_emplacamento = new BigDecimal(250.00);
        BigDecimal valor_cesta_servicos = new BigDecimal(200.00);
        BigDecimal valor_taxa_cadastro = new BigDecimal(130.00);
        String retorno = "1";
        Integer plano_id = 2752;
        String tipoPessoa = "pessoa-fisica";
        BigDecimal valor_bem = new BigDecimal(valorDoBem);
        BigDecimal valor_entrada = new BigDecimal(62910.00);
        BigDecimal valor_parcela_residual = new BigDecimal(31455.00);
        LocalDate dataPrimeiroVencimento = LocalDate.of(2019, 5, 23).plusDays(30);
        BigDecimal valor_seguro_auto = new BigDecimal(0);
        SimulacaoParcResidualRequest request = new SimulacaoParcResidualRequest();
        request.setModalidade(modalidade);
        request.setValorUfEmplacamento(valor_uf_emplacamento);
        request.setValorCestaServicos(valor_cesta_servicos);
        request.setValorTC(valor_taxa_cadastro);
        request.setVlrSeguroFranquia(new BigDecimal(1200));
        request.setRetorno(retorno);
        request.setPlanoId(plano_id);
        request.setTipoPessoa(tipoPessoa);
        request.setValorBem(valor_bem);
        request.setValorEntrada(valor_entrada);
        request.setValorParcelaResidual(valor_parcela_residual);
        request.setDataPrimeiroVencimento(dataPrimeiroVencimento);
        request.setSeguroAuto(valor_seguro_auto);
        request.setCarenciaDoPlano(new Carencia(10, 180));
        request.setParcelas(parcelas);

        ParametrosDeSeguroPrestamista parametroDeSeguro = new ParametrosDeSeguroPrestamista(false, false, true, true,
                true, null, null, true, true, "", Arrays.asList(), null);
        request.setParametrosDeSeguroPrestamista(parametroDeSeguro);

        return request;
    }
}