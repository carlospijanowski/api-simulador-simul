package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.entities.SeguroPrestamistaPlusResponse;
import br.com.bancotoyota.services.simulador.entities.TipoPessoa;
import br.com.bancotoyota.services.simulador.services.SimulacaoServices;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.test.context.junit4.SpringRunner;
import static br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class CustoPercentualDoSeguroPrestamistaTest {

	@MockBean
	private SimulacaoServices simulacaoServices;
	@MockBean
	private SeguroPrestamistaPlusServiceImpl prestamistaService;

	private DadosCalculo getDadosCalculo() {
		return new DadosCalculo(null, null,
				prestamistaService.getSegurosPrestamista("CNPJ", LocalDate.now(), Arrays.asList(), false, false), null,
				null, null, null);
	}

	@Test(expected = HttpMessageConversionException.class)
	public void testaSemParametroDoSeguro() {

		CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(getDadosCalculo(), null, new BigDecimal(90000),
				new BigDecimal(3000), null, TipoPessoa.PESSOA_FISICA);
	}

	private void print(ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista) {
		Exception ex = new Exception();
		System.out.println(ex.getStackTrace()[1].getMethodName());
		System.out.println(parametrosDeSeguroPrestamista);
	}

	@Test
	public void testaSPFForaLimitePorCausaDoPrestamista() {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(false, false,
				true, true, true, null, null, true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(90001), new BigDecimal(2500), null,
				TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.NENHUM, fator.getTipo());
		assertEquals(Boolean.TRUE, fator.getLimiteDeSPFExtrapolado());
	}

	@Test
	public void testaSPF() {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(false, false,
				true, true, true, null, null, true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(87000), new BigDecimal(2500), null,
				TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.SPF, fator.getTipo());
		assertEquals(Boolean.FALSE, fator.getLimiteDeSPFExtrapolado());
		assertNull("SVP nem é avaliado", fator.getLimiteDeSVPExtrapolado());
	}

	@Test
	public void testaPessoaJuridica() {
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), null, new BigDecimal(90000), new BigDecimal(2500), null, TipoPessoa.PESSOA_JURIDICA);
		assertEquals(TipoDeSeguroPrestamista.NENHUM, fator.getTipo());
		assertNull(fator.getLimiteDeSPFExtrapolado());
		assertNull(fator.getLimiteDeSVPExtrapolado());
		assertTrue(BigDecimal.ZERO.compareTo(fator.getFator()) == 0);
	}

	@Test
	public void testaSPFSemParcela() {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(false, false,
				true, true, true, null, null, true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(87000), null, null,
				TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.SPF, fator.getTipo());
		assertEquals(Boolean.FALSE, fator.getLimiteDeSPFExtrapolado());
		assertNull("SVP nem é avaliado", fator.getLimiteDeSVPExtrapolado());
	}

	@Test
	public void testaComCascataDepoisDeRemocao() {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(true, false,
				true, true, true, null, null, true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(8700), new BigDecimal(2500), null,
				TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.SVP, fator.getTipo());
		assertEquals(Boolean.FALSE, fator.getLimiteDeSVPExtrapolado());
		assertEquals("SPF é avaliado pois é necessário ver se poderia ser aplicado para considerar removido",
				Boolean.FALSE, fator.getLimiteDeSPFExtrapolado());
		assertEquals(TipoDeSeguroPrestamista.SVP, fator.getUltimoSeguro().getTipo());
	}

	@Test
	public void testaSemCascataDepoisDeRemocao() {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(true, false,
				true, true, false, null, null, true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(8700), new BigDecimal(2500), null,
				TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.NENHUM, fator.getTipo());
		assertEquals("SPF é avaliado pois só é considerado removido se pudesse ser aplicado", Boolean.FALSE,
				fator.getLimiteDeSPFExtrapolado());
		assertNull("SVP não é considerado pois não tem cascata", fator.getLimiteDeSVPExtrapolado());
		assertNull(fator.getUltimoSeguro());
	}

	@Test
	public void testaSemCascataComValorAltoDeParcela() {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(false, false,
				true, true, false, null, null, true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(88920), new BigDecimal(2501), null,
				TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.SVP, fator.getTipo());
		assertEquals(Boolean.TRUE, fator.getLimiteDeSPFExtrapolado());
		assertEquals(Boolean.FALSE, fator.getLimiteDeSVPExtrapolado());
	}

	@Test
	public void testaSemCascataComValorAltoDeParcelaESpfRemovido() {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(true, false,
				true, true, false, null, null, true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(87000), new BigDecimal(2501), null,
				TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.SVP, fator.getTipo());
		assertTrue(fator.getFator().compareTo(new BigDecimal("0.012000")) == 0);
		assertEquals("spf é avaliado pois só é considerado removido de pudesse ser aplicado", Boolean.TRUE,
				fator.getLimiteDeSPFExtrapolado());
		assertEquals(Boolean.FALSE, fator.getLimiteDeSVPExtrapolado());
	}

	@Test
	public void testaSemCascataComValorAltoDeValorFinanciado() {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(false, false,
				true, true, false, null, null, true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(90001), new BigDecimal(2501), null,
				TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.NENHUM, fator.getTipo());
		assertEquals(Boolean.TRUE, fator.getLimiteDeSPFExtrapolado());
		assertEquals(Boolean.TRUE, fator.getLimiteDeSVPExtrapolado());
	}

	@Test
	public void testaSvpRemovidoComParcelaElevada() {

		BigDecimal limiteDeFinanciamento = new BigDecimal(90000);
		Seguro spf = new Seguro(TipoDeSeguroPrestamista.SPF, new BigDecimal(2500), limiteDeFinanciamento,
				new BigDecimal(450000.00), new BigDecimal("0.029000"), 18, 65);

		List<Seguro> seguros = new LinkedList<>(Arrays.asList(spf));
		when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
				.thenReturn(new SeguroPrestamistaPlusResponse(seguros, seguros.get(0)));

		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(false, true,
				true, true, false, null, null, true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(90000), new BigDecimal(2501), null,
				TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.NENHUM, fator.getTipo());
		assertEquals(true, fator.getLimiteDeSPFExtrapolado());
		assertNull("SVP foi removido e por isso não é avaliado", fator.getLimiteDeSVPExtrapolado());
	}

	@Test
	public void testaSemSeguroPorConfiguracao() {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(false, false,
				false, false, false, null, null, true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(90000), new BigDecimal(2500), null,
				TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.NENHUM, fator.getTipo());
		assertNull("SPF não disponível", fator.getLimiteDeSPFExtrapolado());
		assertNull("SVP não disponível", fator.getLimiteDeSVPExtrapolado());
	}

	@Test
	public void testaSemSeguroPorValorAlto() {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(false, false,
				true, true, true, null, null, true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(90001), new BigDecimal(2500), null,
				TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.NENHUM, fator.getTipo());
		assertEquals(BigDecimal.ZERO, fator.getFator());
		assertEquals(Boolean.TRUE, fator.getLimiteDeSPFExtrapolado());
		assertEquals(Boolean.TRUE, fator.getLimiteDeSVPExtrapolado());
	}

	@Test
	public void testaLimitePorCliente() {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(false, false,
				true, true, true, null, null, true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(50001), new BigDecimal(2500), null,
				TipoPessoa.PESSOA_FISICA);

		assertEquals(TipoDeSeguroPrestamista.SPF, fator.getTipo());
		assertNull("SVP nem é avaliado", fator.getLimiteDeSVPExtrapolado());
		assertEquals(Boolean.FALSE, fator.getLimiteDeSPFExtrapolado());

		parametrosDeSeguroPrestamista.setSpfValorComprometido(new BigDecimal(450000));
		print(parametrosDeSeguroPrestamista);
		fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(getDadosCalculo(), parametrosDeSeguroPrestamista,
				new BigDecimal(50001), new BigDecimal(2500), null, TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.SVP, fator.getTipo());
		assertEquals(Boolean.TRUE, fator.getLimiteDeSPFExtrapolado());
		assertEquals(Boolean.FALSE, fator.getLimiteDeSVPExtrapolado());

		parametrosDeSeguroPrestamista.setSvpValorComprometido(new BigDecimal(990000));
		print(parametrosDeSeguroPrestamista);
		fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(getDadosCalculo(), parametrosDeSeguroPrestamista,
				new BigDecimal(50001), new BigDecimal(2500), null, TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.NENHUM, fator.getTipo());
		assertEquals(BigDecimal.ZERO, fator.getFator());
		assertEquals(Boolean.TRUE, fator.getLimiteDeSPFExtrapolado());
		assertEquals(Boolean.TRUE, fator.getLimiteDeSVPExtrapolado());
	}

	@Test
	public void testaSoSPFDisponivel() {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(false, false,
				true, false, true, null, null, true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(87000), new BigDecimal(2500), null,
				TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.SPF, fator.getTipo());
		assertEquals(Boolean.FALSE, fator.getLimiteDeSPFExtrapolado());
		assertNull("SVP nem é avaliado", fator.getLimiteDeSVPExtrapolado());
	}

	@Test
	public void testaSoSPFDisponivelForaDoLimite() {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(false, false,
				true, false, true, null, null, true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(90000), new BigDecimal(2501), null,
				TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.NENHUM, fator.getTipo());
		assertEquals(Boolean.TRUE, fator.getLimiteDeSPFExtrapolado());
		assertNull("SVP não disponível nem é avaliado", fator.getLimiteDeSVPExtrapolado());
	}

	@Test
	public void testaSemCascataSPFNaoDisponivel() {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(true, false,
				false, true, false, null, null, true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(87000), new BigDecimal(2500), null,
				TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.SVP, fator.getTipo());
		assertNull("SPF não disponível e por isso não é avaliado", fator.getLimiteDeSPFExtrapolado());
		assertEquals(Boolean.FALSE, fator.getLimiteDeSVPExtrapolado());
	}

	@Test
	public void testaSemCascataSPFNaoDisponivelLimiteDeFinanciamento() {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(true, false,
				false, true, false, null, null, true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(90001), new BigDecimal(2500), null,
				TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.NENHUM, fator.getTipo());
		assertNull(fator.getLimiteDeSPFExtrapolado());
		assertEquals(Boolean.TRUE, fator.getLimiteDeSVPExtrapolado());
	}

	@Test
	public void testaSemCascataSPFNaoDisponivelLimiteDoCliente() {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(true, false,
				false, true, false, null, new BigDecimal(990000), true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(90000), new BigDecimal(2500), null,
				TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.NENHUM, fator.getTipo());
		assertEquals("limite do svp deve ter sido extrapolado", Boolean.TRUE, fator.getLimiteDeSVPExtrapolado());
		assertNull("SPF não disponível nem é avaliado", fator.getLimiteDeSPFExtrapolado());
	}

	@Test
	public void testaComCascataSPFNaoDisponivel() {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(true, false,
				false, true, true, null, null, true, true, "", Arrays.asList(), null);
		print(parametrosDeSeguroPrestamista);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(87000), new BigDecimal(2500), null,
				TipoPessoa.PESSOA_FISICA);
		assertEquals(TipoDeSeguroPrestamista.SVP, fator.getTipo());
		assertNull("SPF não disponível e por isso não avaliado", fator.getLimiteDeSPFExtrapolado());
		assertEquals(Boolean.FALSE, fator.getLimiteDeSVPExtrapolado());
	}

	@Before
	public void prepararMockDeSimulacaoServices() {
		BigDecimal limiteDeFinanciamento = new BigDecimal(90000);
		Seguro spf = new Seguro(TipoDeSeguroPrestamista.SPF, new BigDecimal(2500), limiteDeFinanciamento,
				new BigDecimal(450000.00), new BigDecimal("0.029000"), 18, 65);
		Seguro svp = new Seguro(TipoDeSeguroPrestamista.SVP, BigDecimal.ZERO, limiteDeFinanciamento,
				new BigDecimal(990000.00), new BigDecimal("0.012000"), 18, 65);

		List<Seguro> seguros = new LinkedList<>(Arrays.asList(spf, svp));
		when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
				.thenReturn(new SeguroPrestamistaPlusResponse(seguros, seguros.get(1)));
	}

	@Test
	public void testeComTipoEscolhido() {
		// aqui validamos que está dando o tipo mesmo sem passar o tipo escolhido
		testaComTipoEscolhido(null, SPF, 2500, 87000);
		testaComTipoEscolhido(null, SVP, 3000, 88920);
		testaComTipoEscolhido(null, NENHUM, 3000, 91000);

		// aqui garantimos que ao passar o tipo esperado o resultado não se altera
		testaComTipoEscolhido(SPF, SPF, 2500, 87000);
		testaComTipoEscolhido(SVP, SVP, 3000, 88920);
		testaComTipoEscolhido(NENHUM, NENHUM, 3000, 91000);

		// nesse caso poderia ser SPF, mas como escolhemos outro tipo o resultado tem
		// que ser o tipo escolhido
		testaComTipoEscolhido(SVP, SVP, 2500, 87000);
		testaComTipoEscolhido(NENHUM, NENHUM, 2500, 90000);


		testaComTipoEscolhido(NENHUM, NENHUM, 3000, 90000);

		// aqui não pode ser SPF, então apesar do escolhido ser SPF temos que retornar o
		// maior que dá para ser usado
		testaComTipoEscolhido(SPF, SVP, 3000, 88920);

		// aqui só pode ser NENHUM, então não adianta passar que o esperado é diferente
		testaComTipoEscolhido(SPF, NENHUM, 3000, 91000);
		testaComTipoEscolhido(SVP, NENHUM, 3000, 91000);

	}

	// TODO: Teste removido pois esta regra foi despriorizada, deve ser implementada
	// futuramente
	/*
	 * @Test public void testaSPFQuandoEstourouLimiteContratos() {
	 * ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new
	 * ParametrosDeSeguroPrestamista( false, false, true, true, true, null,
	 * null,false,true); print(parametrosDeSeguroPrestamista);
	 * CustoPercentualDoSeguroPrestamista fator =
	 * CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(getDadosCalculo(),
	 * parametrosDeSeguroPrestamista, new BigDecimal(87000), new BigDecimal(2500),
	 * null, TipoPessoa.PESSOA_FISICA); assertEquals(TipoDeSeguroPrestamista.SVP,
	 * fator.getTipo()); assertEquals(Boolean.TRUE,
	 * fator.getLimiteDeSPFExtrapolado()); assertEquals(Boolean.FALSE,
	 * fator.getLimiteDeSVPExtrapolado()); }
	 */
	// TODO: Teste removido pois esta regra foi despriorizada, deve ser implementada
	// futuramente
	/*
	 * @Test public void testaSVPQuandoEstourouLimiteContratos() {
	 * ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new
	 * ParametrosDeSeguroPrestamista( false, false, true, true, true, null,
	 * null,false,false); print(parametrosDeSeguroPrestamista);
	 * CustoPercentualDoSeguroPrestamista fator =
	 * CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(getDadosCalculo(),
	 * parametrosDeSeguroPrestamista, new BigDecimal(87000), new BigDecimal(2500),
	 * null, TipoPessoa.PESSOA_FISICA); assertEquals(TipoDeSeguroPrestamista.NENHUM,
	 * fator.getTipo()); assertEquals(Boolean.TRUE,
	 * fator.getLimiteDeSPFExtrapolado()); assertEquals(Boolean.TRUE,
	 * fator.getLimiteDeSVPExtrapolado()); }
	 */

	public void testaComTipoEscolhido(TipoDeSeguroPrestamista escolhido, TipoDeSeguroPrestamista esperado, int parcela,
									  int valorFinanciado) {
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(false, false,
				true, true, false, null, null, BigDecimal.ZERO, escolhido, true, true, "", Arrays.asList(), null);
		CustoPercentualDoSeguroPrestamista fator = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
				getDadosCalculo(), parametrosDeSeguroPrestamista, new BigDecimal(valorFinanciado),
				new BigDecimal(parcela), null, TipoPessoa.PESSOA_FISICA);
		assertEquals(esperado, fator.getTipo());
	}
}