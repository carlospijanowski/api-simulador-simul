package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.*;
import br.com.bancotoyota.services.simulador.beans.request.*;
import br.com.bancotoyota.services.simulador.beans.response.*;
import br.com.bancotoyota.services.simulador.controller.*;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import org.apache.commons.io.*;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.springframework.test.context.*;
import org.springframework.test.context.junit4.*;

import java.io.*;
import java.math.*;
import java.time.*;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "/application.properties")
public class CalculadoraOfertasImplTest {

    @InjectMocks
    private CalculadoraOfertasImpl calculadoraOfertas;

    @Mock
    private MotorTaxaPlanoService motorTaxaPlanoService;

    @Mock
    private CalculadoraParcelasResidual calculadoraParcelasResidual;

    ObjectMapper mapper = new ObjectMapper();


    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        mapper.findAndRegisterModules();
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Test
    public void calcularOfertas() throws IOException {
        ResultadoParcelaResidual responseTipoA = mapper.readValue(IOUtils.toString(getClass().getClassLoader().getResourceAsStream("desejada/simulacao-tipo-A-tipo-de-subsidio-iguais.json"), "UTF-8"), ResultadoParcelaResidual.class);
        List<Prazo> prazoList = new ArrayList<>();
        SimulacaoParcResidual simulacaoParcResidual = mock(SimulacaoParcResidual.class);

        prazoList.add(new Prazo(12, BigDecimal.ONE, new BigDecimal(20), new BigDecimal(50), new BigDecimal(0), new BigDecimal(0), null));
        prazoList.add(new Prazo(48, BigDecimal.ONE, new BigDecimal(20), new BigDecimal(50), new BigDecimal(0), new BigDecimal(0), null));
        when(motorTaxaPlanoService.getPlanoPorId(any())).thenReturn(mock(PlanoMotorTaxa.class));

        calculadoraOfertas.adicionarIntermediariasNoCalculoDasOfertas(
                criarRequest(),
                responseTipoA,
                false,
                false,
                false,
                LocalDate.now(),
                prazoList,
                TipoDeSeguroPrestamista.SPF,
                false,
                simulacaoParcResidual
        );

    }

    private SimulacaoParcResidualRequest criarRequest() {

        SimulacaoParcResidualRequest request = new SimulacaoParcResidualRequest();
        request.setVlrSeguroFranquia(new BigDecimal(1200));
        request.setDataPrimeiroVencimento(null);
        request.setTrintaDiasAposVencimento(true);
        request.setValorBem(new BigDecimal("100000"));
        request.setValorTC(new BigDecimal(250));
        request.setValorCestaServicos(new BigDecimal(250));
        request.setValorEntrada(new BigDecimal(30000));
        request.setValorUfEmplacamento(new BigDecimal(250));
        request.setParametrosDeSeguroPrestamista(new ParametrosDeSeguroPrestamista(true, false, true, false, true, null, null, true, true, "", Arrays.asList(), null));
        request.setRetorno("1");
        request.setValorParcelaResidual(new BigDecimal(25000));
        request.setCarenciaDoPlano(new Carencia(10, 180));
        request.setTipoPessoa("pessoa-fisica");
        request.setIsentaIOF("N");

        request.setPlanoId(2705);
        return request;
    }

}