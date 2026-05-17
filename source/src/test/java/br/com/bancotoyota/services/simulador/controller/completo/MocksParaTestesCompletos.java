package br.com.bancotoyota.services.simulador.controller.completo;

import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.request.FatorSimulacao;
import br.com.bancotoyota.services.simulador.beans.response.*;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.controller.SimuladorParcDesejadaController;
import br.com.bancotoyota.services.simulador.controller.SimuladorParcDesejadaNovaController;
import br.com.bancotoyota.services.simulador.controller.SimuladorParcResidualController;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.entities.Taxa;
import br.com.bancotoyota.services.simulador.repository.RedisRepository;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.impl.CalculadoraSeguroFranquiaServicesImpl;
import br.com.bancotoyota.services.simulador.services.impl.CalculadoraSeguroMecanicaServicesImpl;
import br.com.bancotoyota.services.simulador.services.impl.CalculadoraServicesImpl;
import br.com.bancotoyota.services.simulador.services.impl.DataCarenciaImpl;
import br.com.bancotoyota.services.simulador.services.impl.SimulacaoServicesImpl;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class MocksParaTestesCompletos {

	protected SimuladorParcResidualController controller;
	protected SimuladorParcDesejadaController desejadaController;
	protected SimuladorParcDesejadaNovaController ncontroller;
	protected SimulacaoServices simulacaoServices;

	private FeatureToggleConfig featureToggleConfig = new FeatureToggleConfig(Boolean.TRUE, Boolean.FALSE);
	protected CalculadoraServices calculadoraServices = new CalculadoraServicesImpl();

	protected CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices = new CalculadoraSeguroMecanicaServicesImpl();

	@MockBean
	protected RedisRepository redisRepository;

	@MockBean
	public PlanoService planoService;

	@MockBean
	public MotorTaxaPlanoService motorTaxaPlanoService;

	@MockBean
	protected ParametrosDeSeguroPrestamistaService parametrosDeSeguroPrestamistaService;

	@MockBean
	protected SeguroPrestamistaPlusServiceImpl prestamistaService;

	@MockBean
	private FatorSimulacao fatorSimulacao;

	private CalculadoraSeguroFranquiaServices seguroFranquiaServices = new CalculadoraSeguroFranquiaServicesImpl();


	@Before
	public void setUp() throws IOException {
		simulacaoServices = new SimulacaoServicesImpl(redisRepository, planoService, motorTaxaPlanoService, featureToggleConfig);
		controller = new SimuladorParcResidualController(calculadoraServices, simulacaoServices, dataCarencia,
				BigDecimal.TEN, parametrosDeSeguroPrestamistaService, false, prestamistaService, featureToggleConfig, seguroFranquiaServices, fatorSimulacao, calculadoraSeguroMecanicaServices);
		desejadaController = new SimuladorParcDesejadaController(calculadoraServices, simulacaoServices, dataCarencia,
				parametrosDeSeguroPrestamistaService, false, prestamistaService, seguroFranquiaServices, calculadoraSeguroMecanicaServices);

		ncontroller = new SimuladorParcDesejadaNovaController(calculadoraServices, simulacaoServices, dataCarencia,
				BigDecimal.TEN, parametrosDeSeguroPrestamistaService, false, prestamistaService, seguroFranquiaServices, calculadoraSeguroMecanicaServices);
		ncontroller.setMapper(mapper);
		ncontroller.setResidualService(new ResidualService(controller));

		ReflectionTestUtils.setField(controller, "motorTaxaPlanoService", motorTaxaPlanoService);

		doNothing().when(motorTaxaPlanoService).applyFatorRating(any());
	}

	public static final String RESIDUAL = "residual/";
	public static final String DESEJADA = "desejada/";

	protected ObjectMapper mapper = new ObjectMapper();

	public MocksParaTestesCompletos() {
		mapper.findAndRegisterModules();
		mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}

	protected DataCarencia dataCarencia = new DataCarenciaImpl(null, null) {
		/**
		 * Fixa a data para a realização dos testes para que possamos trabalhar com
		 * datas fixas.
		 */
		@Override
		public LocalDate getDataCalculo() {
			return getDataAtual();
		}
	};

	protected LocalDate dataAtual = LocalDate.of(2019, 1, 16);

	protected LocalDate getDataAtual() {
		return dataAtual;
	}

	// os métodos a seguir simplesmente fazem o mock do banco de dados

	private Subsidio mockSubsidioVariavel() {
		return Subsidio.builder().tipoSubsidio(EnumTipoSubsidio.VARIAVEL)
				.tipoDistribuicao(EnumTipoDistribuicao.VALOR_PERCENTUAL).vpPercentualDistrMarca(new BigDecimal(0))
				.vpValorMaxMarca(new BigDecimal(9999999)).build();
	}

	private Subsidio mockSubsidioMotorTaxaVariavel() {
		Revendedora rev = new Revendedora();
		rev.setPercentual(new BigDecimal(9999999));
		rev.setValorMaximo(new BigDecimal(9999999));

		FaixasValorMotorTaxas fv = new FaixasValorMotorTaxas();
		fv.setSequencial(1);
		fv.setResponsabilidade(ResponsabilidadeMotorTaxa.DEALER);
		fv.setValorInicial(new BigDecimal(1));
		fv.setValorFinal(new BigDecimal(1000));

		return convertSubsidioFromMotorTaxa(SubsidioMotorTaxa.builder()
				.tipoSubsidio(TipoSubsidio.FIXO)
				.tipoDistribuicao(TipoDistribuicao.FAIXA)  //V-valor  P-Percentual
				.revendedora(rev)
				.montadora(Montadora.builder().percentual(new BigDecimal(9999999)).valorMaximo(new BigDecimal(9999999)).build())
				.faixasValorMotorTaxas(Arrays.asList(fv))
				.build());

	}

	private Subsidio mockSubsidioFixo() {
		return Subsidio.builder().tipoSubsidio(EnumTipoSubsidio.FIXO).fixoPercentualMontadora(new BigDecimal(50))
				.fixoPercentualRevendedora(new BigDecimal(50)).build();
	}

	private SubsidioMotorTaxa mockSubsidioMotorTaxaFixo() {
		Revendedora rev = new Revendedora();
		rev.setPercentual(new BigDecimal(9999999));
		rev.setValorMaximo(new BigDecimal(9999999));

		FaixasValorMotorTaxas fv = new FaixasValorMotorTaxas();
		fv.setSequencial(1);
		fv.setResponsabilidade(ResponsabilidadeMotorTaxa.DEALER);
		fv.setValorInicial(new BigDecimal(1));
		fv.setValorFinal(new BigDecimal(1000));

		return SubsidioMotorTaxa.builder()
				.tipoSubsidio(TipoSubsidio.FIXO)
				.tipoDistribuicao(TipoDistribuicao.PERCENTUAL)
				.revendedora(rev)
				.montadora(new Montadora(new BigDecimal(9999999), new BigDecimal(9999999),new BigDecimal(9999999)))
				.faixasValorMotorTaxas(Arrays.asList(fv))
				.build();
	}

	private Plano mockPlanoMotorTaxaVariavel() {

		return 	Plano.builder().codigoPlano("2834")

				.build();
	}


	public void prepararMockDeSimulacaoServices(Collection<Integer> prazos) {

		reset(redisRepository);
		reset(planoService);
		reset(prestamistaService);
		reset(motorTaxaPlanoService);

		doNothing().when(motorTaxaPlanoService).applyFatorRating(any());

		when(planoService.findSubsidio(any())).then(i -> {
			Integer plano = i.getArgument(0);
			if (plano == 2710) {
				return mockSubsidioVariavel();
			} else if (plano == 2807) {
				return mockSubsidioVariavel();
			} else if (plano == 2808) {
				return mockSubsidioFixo();
			} else {
				return null;
			}
		});

		when(motorTaxaPlanoService.findSubsidio(any())).then(i -> {
			Integer plano = i.getArgument(0);
			if (plano == 2710) {
				return mockSubsidioMotorTaxaVariavel();
			} else if (plano == 2807) {
				return mockSubsidioMotorTaxaVariavel();
			} else if (plano == 2808) {
				return mockSubsidioMotorTaxaFixo();
			} else {
				return null;
			}
		});

		when(motorTaxaPlanoService.getPlanoPorId(any())).then(i -> {
			if (i == null) {
				return null;
			}
			Integer plano = i.getArgument(0);
			if (plano == null) {
				return null;
			}else if (plano == 2834) {
				return mockPlanoMotorTaxaVariavel();
			} else if (plano == 2807) {
				return mockPlanoMotorTaxaVariavel();
			} else if (plano == 2808) {
				return mockPlanoMotorTaxaVariavel();
			} else {
				return null;
			}
		});



		when(planoService.findPrazos(any(), any())).then(i -> {
			Integer plano = i.getArgument(0);
			Collection<Integer> meses = i.getArgument(1);
			if (meses == null || meses.isEmpty()) {
				meses = prazos;
			}
			if (plano == 2709) {
				return dadosDoPlano2709(meses);
			} else if (plano == 2711) {
				return dadosDoPlano2709B(meses);
			} else if (plano == 2710) {
				return dadosDoPlano2709(meses);
			} else if (plano == 2806) {
				return dadosDoPlano2806(meses);
			} else if (plano == 2807) {
				return dadosDoPlano2806(meses);
			} else if (plano == 2808) {
				return dadosDoPlano2806(meses);
			} else if (plano == 2812) {
				return dadosDoPlano2812(meses);
			} else if (plano == 2813) {
				return dadosDoPlano2813(meses);
			} else if (plano == 2752) {
				return dadosDoPlano2812(meses);
			} else {
				throw new IllegalStateException("plano " + plano + " não mockado");
			}
		});


		when(motorTaxaPlanoService.findPrazos(any(), any())).then(i -> {
			Integer plano = i.getArgument(0);
			Collection<Integer> meses = i.getArgument(1);
			if (meses == null || meses.isEmpty()) {
				meses = prazos;
			}
			if (plano == 2709) {
				return dadosDoPlano2709(meses);
			} else if (plano == 2711) {
				return dadosDoPlano2709B(meses);
			} else if (plano == 2710) {
				return dadosDoPlano2709(meses);
			} else if (plano == 2806) {
				return dadosDoPlano2806(meses);
			} else if (plano == 2807) {
				return dadosDoPlano2806(meses);
			} else if (plano == 2808) {
				return dadosDoPlano2806(meses);
			} else if (plano == 2812) {
				return dadosDoPlano2812(meses);
			} else if (plano == 2813) {
				return dadosDoPlano2813(meses);
			} else if (plano == 2752) {
				return dadosDoPlano2812(meses);
			} else {
				throw new IllegalStateException("plano " + plano + " não mockado");
			}
		});

		when(motorTaxaPlanoService.findTaxas(any())).then(i -> {
			Integer plano = i.getArgument(0);

			if (plano == 2709) {
				return dadosTaxaMT2709();
			} else if (plano == 2711) {
				return dadosTaxaMT2709();
			} else if (plano == 2710) {
				return dadosTaxaMT2709();
			} else if (plano == 2806) {
				return dadosTaxaMT2709();
			} else if (plano == 2807) {
				return dadosTaxaMT2709();
			} else if (plano == 2808) {
				return dadosTaxaMT2709();
			} else if (plano == 2812) {
				return dadosTaxaMT2709();
			} else if (plano == 2813) {
				return dadosTaxaMT2709();
			} else if (plano == 2752) {
				return dadosTaxaMT2709();
			} else {
				throw new IllegalStateException("plano " + plano + " não mockado");
			}
		});

		List<Seguro> seguros =getSegurosPrestamista();
		when(redisRepository.findTaxasIOF(anyString())).then(i -> getTaxasIOF());
		when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
				.thenReturn(new SeguroPrestamistaPlusResponse(seguros, seguros.get(1)));
		when(prestamistaService.getSeguroPrestamista(any(), any(),any())).thenAnswer(p -> {
			TipoDeSeguroPrestamista tipoPrestamista = p.getArgument(2,TipoDeSeguroPrestamista.class);
			if(tipoPrestamista.equals(TipoDeSeguroPrestamista.SPF)) {
				return Optional.of(seguros.get(0));
			}
			else {
				return Optional.of(seguros.get(1));
			}

		});
	}

	public static TaxasIOF getTaxasIOF() {
		return new TaxasIOF("365.000000", "0.030000", "0.003800");
	}

	public static List<Seguro> getSegurosPrestamista() {
    	Seguro svp = new  Seguro(TipoDeSeguroPrestamista.SVP, BigDecimal.ZERO, new BigDecimal("330000.00"), new BigDecimal("990000.00"), new BigDecimal("0.012000"), 18, 65);
    	Seguro spf = new Seguro(TipoDeSeguroPrestamista.SPF, new BigDecimal(2500), new BigDecimal("150000.00"), new BigDecimal("450000.00"), new BigDecimal("0.029000"), 18, 65);

    	spf.setOrdenacao(1);
    	svp.setOrdenacao(2);
    	LinkedList<Seguro> seguros = new LinkedList<>(Arrays.asList(spf,
               svp));
        return seguros;
    }

	protected List<Prazo> dadosDoPlano2709(Collection<Integer> prazos) throws IOException {
		List<Prazo> list = dadosDoPlano2709();
		return list.stream().filter(p -> prazos.contains(p.getNumeroDeParcelas())).collect(Collectors.toList());
	}

	protected Taxa dadosTaxaMT2709() throws IOException {
		Taxa taxa = dadosTaxaMT2709X();
		return taxa;
	}

	protected List<Prazo> dadosDoPlano2709B(Collection<Integer> prazos) throws IOException {
		List<Prazo> list = dadosDoPlano2709B();
		return list.stream().filter(p -> prazos.contains(p.getNumeroDeParcelas())).collect(Collectors.toList());
	}

	protected List<Prazo> dadosDoPlano2806(Collection<Integer> prazos) throws IOException {
		List<Prazo> list = dadosDoPlano2806();
		return list.stream().filter(p -> prazos.contains(p.getNumeroDeParcelas())).collect(Collectors.toList());
	}

	protected List<Prazo> dadosDoPlano2812(Collection<Integer> prazos) throws IOException {
		List<Prazo> list = dadosDoPlano2812();
		return list.stream().filter(p -> prazos.contains(p.getNumeroDeParcelas())).collect(Collectors.toList());
	}

	protected List<Prazo> dadosDoPlano2813(Collection<Integer> prazos) throws IOException {
		List<Prazo> list = dadosDoPlano2812();
		return list.stream().filter(p -> prazos.contains(p.getNumeroDeParcelas())).map(p -> {
			p.setPercentualMinBalao(BigDecimal.ZERO);
			return p;
		}).collect(Collectors.toList());
	}

	protected List<Prazo> dadosDoPlano2806() throws IOException {
		String json = lerArquivo("plano2806.json", "./");
		Prazo[] prazos = mapper.readValue(json, getArrayClass(Prazo.class));
		return Arrays.asList(prazos);
	}

	protected List<Prazo> dadosDoPlano2812() throws IOException {
		String json = lerArquivo("plano2812.json", "./");
		Prazo[] prazos = mapper.readValue(json, getArrayClass(Prazo.class));
		return Arrays.asList(prazos);
	}

	protected List<Prazo> dadosDoPlano2709() throws IOException {
		String json = lerArquivo("plano2709.json", "./");
		Prazo[] prazos = mapper.readValue(json, getArrayClass(Prazo.class));
		return Arrays.asList(prazos);
	}

	protected Taxa dadosTaxaMT2709X() throws IOException {
		String json = lerArquivo("taxaMT2709.json", "./");
		Taxa taxa = mapper.readValue(json, Taxa.class);
		return taxa;
	}

	protected List<Prazo> dadosDoPlano2709B() throws IOException {
		String json = lerArquivo("plano2709.json", "./");
		Prazo[] prazos = mapper.readValue(json, getArrayClass(Prazo.class));
		for (Prazo p : prazos) {
			p.setPercentualMaxBalao(BigDecimal.ZERO);
			p.setPercentualMinBalao(BigDecimal.ZERO);
		}
		return Arrays.asList(prazos);
	}

	protected String lerArquivo(String nome, String basePath) throws IOException {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(basePath + nome)) {
			if (in == null) {
				throw new RuntimeException("arquivo não encontrado: " + basePath + nome);
			}
			return IOUtils.toString(in, "UTF-8");
		}
	}

	static <T> Class<? extends T[]> getArrayClass(Class<T> clazz) {
		return (Class<? extends T[]>) Array.newInstance(clazz, 0).getClass();
	}

	private Subsidio convertSubsidioFromMotorTaxa(SubsidioMotorTaxa subsidioMotorTaxa) {

		return Subsidio.builder()
				.tipoSubsidio(EnumTipoSubsidio.VARIAVEL)  // Verificar F ou V
				.taxaBanco(subsidioMotorTaxa.getTaxaBanco())
				.fixoPercentualRevendedora(subsidioMotorTaxa.getRevendedora().getPercentual())
				.fixoPercentualMontadora(subsidioMotorTaxa.getMontadora().getPercentual())
				.tipoDistribuicao(EnumTipoDistribuicao.VALOR_PERCENTUAL)  // Verificar P- "Percentual" ou F - "Faixa"
				.faixasValor(convertTaxaValor(subsidioMotorTaxa.getFaixasValorMotorTaxas()))
				.build();

	}

	private List<Subsidio.FaixaValor> convertTaxaValor(List<FaixasValorMotorTaxas> faixasValorMotorTaxas) {
		List<Subsidio.FaixaValor> listaFaixaValor = new ArrayList<>();
		faixasValorMotorTaxas.forEach(faixaValor -> {
					listaFaixaValor.add(Subsidio.FaixaValor.builder()
							.sequencial(faixaValor.getSequencial())
							.responsabilidade(EnumResponsabilidade.DEALER) // Verificar
							.valorInicial(faixaValor.getValorInicial())
							.valorFinal(faixaValor.getValorFinal())
							.build());
				}
		);
		return listaFaixaValor;
	}


}
