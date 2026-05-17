package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.ItemFinanciavel;
import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.request.DadosContatoRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcDesejadaRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoSimplificadaDesejadaRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoSimplificadaResidualRequest;
import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.controller.completo.MocksParaTestesCompletos;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.repository.RedisRepository;
import br.com.bancotoyota.services.simulador.repository.propostas.DataBARepository;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.exceptions.HttpStatusInvalidException;
import br.com.bancotoyota.services.simulador.services.impl.CalculadoraServicesImpl;
import br.com.bancotoyota.services.simulador.services.impl.DataCarenciaImpl;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import br.com.bancotoyota.services.simulador.utils.LocalDateTimeSerializer;

import org.apache.commons.io.IOUtils;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public abstract class SimuladorSimplificadoControllerBaseTest  {

    static final String TOKEN = "token";
    static final String ORIGEM_NEGOCIO_CNPJ = "01234567000189";
    static final String CLIENTE_ATIVO_CPF = "01234567890";
    static final String CLIENTE_INATIVO_CPF = "09876543210";
    static final String PLANO_ID = "2752";
    public static final String MARCA_ID = "002";
    static final String MODELO_ID = "001265";
    public static final Integer CARENCIA_MIN = 10;
    public static final Integer CARENCIA_MAX = 180;
    static final Integer[] PRAZOS = new Integer[] { 12, 24, 36 };
    static final BigDecimal TAXA_MES = new BigDecimal("1.308033");
    static final BigDecimal VINTE_PERC = new BigDecimal("20.00");
    static final BigDecimal QUARENTA_PERC = new BigDecimal("40.00");
    public static final BigDecimal CINQUENTA_PERC = new BigDecimal("50.00");

    private FeatureToggleConfig featureToggleConfig = new FeatureToggleConfig(Boolean.TRUE, Boolean.FALSE);

    BigDecimal subsidioMinimoPorValor = BigDecimal.valueOf(10);
    CalculadoraServices calculadoraService = new CalculadoraServicesImpl();
    public DataCarencia dataCarencia;
    @MockBean
    public DataBARepository dataBARepository;
    @MockBean
    SimulacaoServices simulacaoService;
    @MockBean
    ParametrosDeSeguroPrestamistaService parametrosDeSeguroPrestamistaService;
    @MockBean
    public EmplacamentoService emplacamentoService;
    @MockBean
    public OrigemNegocioService origemNegocioService;
    @MockBean
    public CestaServicoService cestaServicoService;
    @MockBean
    public PlanoService planoService;
    @MockBean
    public MotorTaxaPlanoService motorTaxaPlanoService;
    @MockBean
    public ClientService clientService;
    @MockBean
    public VeiculosService veiculosService;
    @MockBean
    public SimulacaoSimplificadaService simulacaoSimplificadaService;    
    @MockBean
    public PropostasFinanciamentoService propostasFinanciamentoService;    
    @MockBean
    protected RedisRepository redisRepository;
    @MockBean
    public SeguroPrestamistaPlusServiceImpl prestamistaService;
    
    public void setup() throws HttpStatusInvalidException {
    	doNothing().when(motorTaxaPlanoService).applyFatorRating(any());
    	
        dataCarencia = new DataCarenciaImpl(dataBARepository,redisRepository);
        when(dataBARepository.findAll()).thenReturn(Arrays.asList(new DataBA(LocalDate.now())));
        ControlesBA controleBA = new ControlesBA();
        controleBA.setDataAtualBA(LocalDateTime.now());
        when(redisRepository.getDataBA()).thenReturn(controleBA);

        List<Prazo> prazoList = new ArrayList<>();
        prazoList.add(new Prazo(12, TAXA_MES, VINTE_PERC, CINQUENTA_PERC, VINTE_PERC, CINQUENTA_PERC,null));
        prazoList.add(new Prazo(24, TAXA_MES, VINTE_PERC, CINQUENTA_PERC, VINTE_PERC, CINQUENTA_PERC,null));
        prazoList.add(new Prazo(36, TAXA_MES, VINTE_PERC, QUARENTA_PERC, VINTE_PERC, CINQUENTA_PERC,null));
        TaxasIOF taxasIOF = new TaxasIOF("365.000000", "0.030000", "0.003800");
        BigDecimal limiteDeFinanciamento = new BigDecimal(90000);
        Seguro spf = new Seguro(TipoDeSeguroPrestamista.SPF, new BigDecimal(2500), limiteDeFinanciamento,
                new BigDecimal(450000.00), new BigDecimal("0.029000"), 18, 65);
        Seguro svp = new Seguro(TipoDeSeguroPrestamista.SVP, BigDecimal.ZERO, limiteDeFinanciamento,
                new BigDecimal(990000.00), new BigDecimal("0.012000"), 18, 65);

        List<Seguro> seguros = Arrays.asList(spf, svp);
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(seguros, seguros.get(1)));

        when(simulacaoService.getTaxasIOF(any(), anyString())).then(i -> taxasIOF);
        when(simulacaoService.getParcelas(anyInt(), any(), any(), isNull(), isNull())).then(i -> filtrarPlazos(prazoList, i.getArgument(2)));

        Estado estado = new Estado("SP", "São Paulo", new BigDecimal(450.00), "500", LocalDateTime.of(2019, 8, 15, 9, 26));
        when(emplacamentoService.getEstado(eq(UfEmplacamento.SP), any())).thenReturn(estado);

        OrigemNegocio origemNegocio = new OrigemNegocio("123", ORIGEM_NEGOCIO_CNPJ, "São Paulo", "SP", "A", ORIGEM_NEGOCIO_CNPJ,
                "Banco Toyota", ORIGEM_NEGOCIO_CNPJ, "SP CAPITAL", false, true, true, true, true, true);
        when(origemNegocioService.getOrigemNegocio(eq(ORIGEM_NEGOCIO_CNPJ), any())).thenReturn(origemNegocio);

        CestaServico cestaServico = new CestaServico(1, "Cesta de Serviço de Teste", new BigDecimal(1050), false, true, true, null);
        when(cestaServicoService.getCestaServicoMaisCara(eq(ORIGEM_NEGOCIO_CNPJ), eq(Modalidade.CDC),
        		eq(TipoPessoa.PESSOA_FISICA), eq(null), any())).thenReturn(cestaServico);
        when(cestaServicoService.getCestaServicoMaisCara(eq(ORIGEM_NEGOCIO_CNPJ), eq(Modalidade.CDC),
        		eq(TipoPessoa.PESSOA_FISICA), eq("COORDENADOR"), any())).thenReturn(cestaServico);

        List<CestaItem> cestaItems = new ArrayList<>();
        cestaItems.add(new CestaItem(1, new BigDecimal(500), 1, "CONFECÇÃO DE CADASTRO - PF", true));
        cestaItems.add(new CestaItem(2, new BigDecimal(550), 1, "TARIFA DE ADITAMENTOS", false));
        when(cestaServicoService.getDespesas(eq(1), any())).thenReturn(cestaItems);

        Plano plano = new Plano("ciclo-toyota", PLANO_ID, "1", "CICLO LXT>40 121", 2018, 9999,
                LocalDateTime.parse("2019-03-14 00:00:00.000", LocalDateTimeSerializer.DATE_TIME_FORMATTER),
                new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList(MARCA_ID)), new BigDecimal("40.000000"), new BigDecimal("100.000000"),
                new BigDecimal("0.00"), CINQUENTA_PERC, CARENCIA_MIN, CARENCIA_MAX, "CDC-TCM", false, null, false,
                "AI_E_0KM", 30, "12", "N", "MENSAL", 1, true, false,EnumControleBalao.PERMITE_BALAO,null,null,null);
        when(planoService.getPlanoCicloToyota(any(), any(), any(), anyString())).thenReturn(plano);
        when(motorTaxaPlanoService.getPlanoCicloToyota(any(), any(), any()))
                .thenReturn(plano);


        when(motorTaxaPlanoService.convertPlanoFromMotorTaxa(motorTaxaPlanoService.getPlanoPorId(any()))).thenReturn(plano);
        when(planoService.getPlanoById(any())).thenReturn(plano);
        Retorno retorno = new Retorno("0", "0", "100");
        Retorno retorno1 = new Retorno("1", "0", "100");
        when(planoService.getRetorno(eq(PLANO_ID), eq(ORIGEM_NEGOCIO_CNPJ), eq(null), any())).thenReturn(retorno);
        when(planoService.getRetorno(eq(PLANO_ID), eq(ORIGEM_NEGOCIO_CNPJ), eq("COORDENADOR"), any())).thenReturn(retorno);
        when(planoService.getRetorno(eq("2709"), eq(ORIGEM_NEGOCIO_CNPJ), eq("COORDENADOR"), any())).thenReturn(retorno);
        when(planoService.getRetorno(eq(PLANO_ID), eq(ORIGEM_NEGOCIO_CNPJ), eq("COORDENADOR"),eq("1"), any())).thenReturn(retorno1);
        when(planoService.getRetorno(eq(PLANO_ID), eq(ORIGEM_NEGOCIO_CNPJ), eq(null),eq("1"), any())).thenReturn(retorno1);

        when(motorTaxaPlanoService.getRetorno(eq(PLANO_ID), eq(ORIGEM_NEGOCIO_CNPJ), eq(null))).thenReturn(retorno);
        when(motorTaxaPlanoService.getRetorno(eq(PLANO_ID), eq(ORIGEM_NEGOCIO_CNPJ), eq("COORDENADOR"))).thenReturn(retorno);
        when(motorTaxaPlanoService.getRetorno(eq("2709"), eq(ORIGEM_NEGOCIO_CNPJ), eq("COORDENADOR"))).thenReturn(retorno);
        when(motorTaxaPlanoService.getRetorno(eq(PLANO_ID), eq(ORIGEM_NEGOCIO_CNPJ), eq("COORDENADOR"),eq("1"))).thenReturn(retorno1);
        when(motorTaxaPlanoService.getRetorno(eq(PLANO_ID), eq(ORIGEM_NEGOCIO_CNPJ), eq(null),eq("1"))).thenReturn(retorno1);

        ModeloVeiculos  modeloVeiculo = new ModeloVeiculos("2020", "2020", "V", "001265", "001265-3", "COROLLA ALTIS 2.0 FLEX 16V AUT.", "VEICULOS", "001265-3$001", new MarcaVeiculos("TOYOTA", "002"), "CARGA"); 
        when(veiculosService.getModelo(any(), any(), any(), any())).thenReturn(modeloVeiculo);
        when(veiculosService.getModeloZeroKm(any(), any(), any(), any())).thenReturn(modeloVeiculo);
        
        ValorComprometidoSeguroPrestamista.ClienteComprometimentoSeguro clienteComprometimentoSeguro =
        		new ValorComprometidoSeguroPrestamista.ClienteComprometimentoSeguro(BigDecimal.ZERO, BigDecimal.ZERO);
        when(clientService.getValoresComprometidos(any())).thenReturn(clienteComprometimentoSeguro);
        when(clientService.isAtivo(CLIENTE_ATIVO_CPF)).thenReturn(true);
        when(clientService.isAtivo(CLIENTE_INATIVO_CPF)).thenReturn(false);
        
        SimulacaoSimplificada simulacaoSimplificada = new SimulacaoSimplificada();
        simulacaoSimplificada.setId("SIMULACAO_SIMPLIFICADA_TEST");
        when(simulacaoSimplificadaService.simplificadaDesejadaToSimulacaoSimplificada(any(), any(), any(), any())).thenReturn(simulacaoSimplificada);
        when(simulacaoSimplificadaService.simplificadaResidualToSimulacaoSimplificada(any(), any(), any(), any())).thenReturn(simulacaoSimplificada);
        when(simulacaoSimplificadaService.findByCodigoSimulacaoSessionIdOrigemPrazo(any(), any(), any(), any())).thenReturn(simulacaoSimplificada);
        when(simulacaoSimplificadaService.insertSimulacaoSimplificada(any())).thenReturn("SIMULACAO_SIMPLIFICADA_TEST");
        try {
        	ObjectMapper mapper = new ObjectMapper();
			SimulacaoSimplificada simulacaoSimplificada1 = mapper.readValue(
			        lerArquivo(getClass().getClassLoader().getResourceAsStream("dados-simulacao-simplificada.json")), SimulacaoSimplificada.class);
			
			when(simulacaoSimplificadaService.findByCodigoSimulacao(any())).thenReturn(simulacaoSimplificada1);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        when(propostasFinanciamentoService.gerarTextoAvisoLegalSimulacaoSimplificada(any(), any())).thenReturn("Texto Aviso Legal");
        
        
    }

    protected List<Prazo> filtrarPlazos(List<Prazo> prazoList, Collection<Integer> meses) {
        if (meses != null && !meses.isEmpty()) {
            return prazoList.stream().filter(p -> meses.contains(p.getNumeroDeParcelas())).collect(Collectors.toList());
        } else {
            return prazoList;
        }
    }

    protected String lerArquivo(InputStream resource) throws IOException {
        return IOUtils.toString(resource, "UTF-8");
    }    
    
    SimulacaoParcDesejadaRequest criarSimulacaoParcDesejadaRequest(BigDecimal valor_parcela_desejada) {
        SimulacaoParcDesejadaRequest request = new SimulacaoParcDesejadaRequest();
        request.setValorUfEmplacamento(BigDecimal.valueOf(450.00));
        request.setValorCestaServicos(BigDecimal.valueOf(550.00));
        request.setValorTC(BigDecimal.valueOf(500.00));
        request.setPlanoId(Integer.parseInt(PLANO_ID));
        request.setTipoPessoa(TipoPessoa.PESSOA_FISICA.getValue());
        request.setValorBem(BigDecimal.valueOf(100000));
        request.setValorEntrada(BigDecimal.valueOf(40000));
        request.setVlrSeguroFranquia(new BigDecimal(1200));
        request.setValorParcelaDesejada(valor_parcela_desejada);
        request.setDataPrimeiroVencimento(LocalDate.now().plusDays(30));
        request.setCarenciaDoPlano(new Carencia(CARENCIA_MIN, CARENCIA_MAX));
        request.setNDiasAposVencimento(false);

        ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista =
                new ParametrosDeSeguroPrestamista(false, false, true, true, true, null, null,true,true, "", Arrays.asList(),null);
        request.setParametrosDeSeguroPrestamista(parametrosDeSeguroPrestamista);
        request.setRetorno("0");
        request.setIsentaIOF("N");
        return request;
    }

    public SimulacaoSimplificadaDesejadaRequest criarSimulacaoSimplificadaDesejadaRequest(
            BigDecimal valor_parcela_desejada) {
        return criarSimulacaoSimplificadaDesejadaRequest(valor_parcela_desejada, null);
    }

    SimulacaoSimplificadaDesejadaRequest criarSimulacaoSimplificadaDesejadaRequest(
            BigDecimal valor_parcela_desejada, String cpfCnpjCliente) {
        SimulacaoSimplificadaDesejadaRequest request = new SimulacaoSimplificadaDesejadaRequest();
        request.setUfEmplacamento(UfEmplacamento.SP);
        request.setValorSeguroFranquia(new BigDecimal(1200));
        request.setCnpjOrigemNegocio(ORIGEM_NEGOCIO_CNPJ);
        request.setValorBem(BigDecimal.valueOf(100000));
        request.setValorEntrada(BigDecimal.valueOf(40000));
        request.setMarcaId(MARCA_ID);
        request.setModeloId(MODELO_ID);
        request.setValorParcelaDesejada(valor_parcela_desejada);
        request.setDataPrimeiroVencimento(LocalDate.now().plusDays(30));
        request.setCpfCnpjCliente(cpfCnpjCliente);
        return request;
    }

    SimulacaoParcResidualRequest criarSimulacaoParcResidualRequest(Integer[] parcelas,
                                                                   BigDecimal valor_parcela_residual) {
        SimulacaoParcResidualRequest request = new SimulacaoParcResidualRequest();
        request.setValorUfEmplacamento(BigDecimal.valueOf(450.00));
        request.setValorCestaServicos(BigDecimal.valueOf(550.00));
        request.setValorTC(BigDecimal.valueOf(500.00));
        request.setPlanoId(Integer.parseInt(PLANO_ID));
        request.setTipoPessoa(TipoPessoa.PESSOA_FISICA.getValue());
        request.setValorBem(BigDecimal.valueOf(100000));
        request.setValorEntrada(BigDecimal.valueOf(40000));
        request.setValorParcelaResidual(valor_parcela_residual);
        request.setVlrSeguroFranquia(new BigDecimal(1200));
        request.setDataPrimeiroVencimento(LocalDate.now().plusDays(30));
        request.setSeguroAuto(BigDecimal.ZERO);
        request.setCarenciaDoPlano(new Carencia(CARENCIA_MIN, CARENCIA_MAX));
        request.setParcelas(parcelas);
        request.setRetorno("0");
        request.setIsentaIOF("N");

        ParametrosDeSeguroPrestamista parametroDeSeguro =
                new ParametrosDeSeguroPrestamista(false, false, true, true, true, null, null,true,true, "", Arrays.asList(),null);
        request.setParametrosDeSeguroPrestamista(parametroDeSeguro);
        return request;
    }

    public SimulacaoSimplificadaResidualRequest criarSimulacaoSimplificadaResidualRequest(Integer[] parcelas,
                                                                                   BigDecimal valor_parcela_residual) {
        return criarSimulacaoSimplificadaResidualRequest(parcelas, valor_parcela_residual, null);
    }

    SimulacaoSimplificadaResidualRequest criarSimulacaoSimplificadaResidualRequest(Integer[] parcelas,
                                                                                   BigDecimal valor_parcela_residual,
                                                                                   String cpjCnpjCliente) {
        SimulacaoSimplificadaResidualRequest request = new SimulacaoSimplificadaResidualRequest();
        request.setUfEmplacamento(UfEmplacamento.SP);
        request.setCnpjOrigemNegocio(ORIGEM_NEGOCIO_CNPJ);
        request.setValorBem(BigDecimal.valueOf(100000));
        request.setValorEntrada(BigDecimal.valueOf(40000));
        request.setMarcaId(MARCA_ID);
        request.setModeloId(MODELO_ID);
        request.setDataPrimeiroVencimento(LocalDate.now().plusDays(30));
        request.setParcelas(parcelas);
        request.setValorParcelaResidual(valor_parcela_residual);
        request.setSeguroAuto(BigDecimal.ZERO);
        request.setValorSeguroFranquia(new BigDecimal(1200));
        request.setCpfCnpjCliente(cpjCnpjCliente);
        return request;
    }

    ItemFinanciavel getItemFinanciavel(int codigo, BigDecimal valor){
        ItemFinanciavel i = new ItemFinanciavel();
        i.setCodigo(codigo);
        i.setValor(valor);
        return i;
    }
    
    DadosContatoRequest criarDadosContatoRequest(String sessionId, String cpfCnpj, String idOfertaSalesforce, String origem, Integer prazo) {
    	DadosContatoRequest dadosContatoRequest = new DadosContatoRequest();
    	dadosContatoRequest.setCelular("1199999999");
    	dadosContatoRequest.setCpfCnpj(cpfCnpj);
    	dadosContatoRequest.setEmail("teste@simplificada.com.br");
    	dadosContatoRequest.setIdOfertaSalesforce(idOfertaSalesforce);
    	dadosContatoRequest.setNome("Teste Simplificada");
    	dadosContatoRequest.setOrigem(origem);
    	dadosContatoRequest.setPrazo(prazo);
    	dadosContatoRequest.setSessionId(sessionId);
    	dadosContatoRequest.setTelefone("1199999999");
    	dadosContatoRequest.setCodigoSimulacao("9999999999999999999");;
    	return dadosContatoRequest;
    }

    
}
