package br.com.bancotoyota.services.simulador.entities;

import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

public class EntitiesTest {

	@Test
	public void cestaItem() {
		CestaItem cestaItem = new CestaItem();
		cestaItem = new CestaItem(1, new BigDecimal(1), 1, "item 1", true);

		cestaItem.getCodigoDespesa();
		cestaItem.getDescricao();
		cestaItem.getIsentaClienteAtivo();
		cestaItem.getQuantidade();
		cestaItem.getValorUnitario();

		assertNotNull(cestaItem);

	}

	@Test
	public void messageHolder() {

		MessageHolder m = new MessageHolder();

		m.setMessage("message");

		m.getMessage();

		assertNotNull(m);
	}

	@Test
	public void modeloVeiculos() {

		ModeloVeiculos modelo = new ModeloVeiculos();
		modelo = new ModeloVeiculos("anoFabricacao", "anoModelo", "codigoCategoria", "codigoModelo", "codigoMolicar",
				"descricaoModelo", "descricaoCategoria", "idModelo", new MarcaVeiculos("TOYOTA", "002"),
				"tipoRegistro");

		modelo.getAnoFabricacao();
		modelo.getAnoModelo();
		modelo.getCodigoCategoria();
		modelo.getCodigoModelo();
		modelo.getCodigoMolicar();
		modelo.getDescricaoCategoria();
		modelo.getDescricaoModelo();
		modelo.getIdModelo();
		modelo.getMarca().getDescricao();
		modelo.getMarca().getId();
		modelo.getTipoRegistro();

		assertNotNull(modelo);
	}

	@Test
	public void propostaAvisoLegal() {
		
		ConfiguracaoAvisoLegal conf = new ConfiguracaoAvisoLegal();
		conf = new ConfiguracaoAvisoLegal("CDC", "PESSOA_FISICA", "0km", "CDC", LocalDate.now());
		
		conf.getDataCalculo();
		conf.getModalidade();
		conf.getSituacaoVeiculo();
		conf.getTipoFinanciamento();
		conf.getTipoPessoa();
		
		assertNotNull(conf);		
		
		ModeloAvisoLegal modeloAvisoLegal = new ModeloAvisoLegal();
		
		modeloAvisoLegal = new ModeloAvisoLegal("2020", "002121", "002121-9", "DESCRICAO");
		
		modeloAvisoLegal.getAnoModelo();
		modeloAvisoLegal.getCodigoModelo();
		modeloAvisoLegal.getCodigoMolicar();
		modeloAvisoLegal.getDescricaoModelo();

		assertNotNull(modeloAvisoLegal);	
		
		MarcaAvisoLegal m = new MarcaAvisoLegal();
		m = new MarcaAvisoLegal("TOYOTA", "002");
		
		m.getDescricao();
		m.getId();
		
		assertNotNull(m);		
		
		DiaParcela diaParcela = new DiaParcela();
		
		diaParcela.setId(1);
		diaParcela.setDescricao("diaParcela");
		
		diaParcela.getDescricao();
		diaParcela.getId();
		
		assertNotNull(diaParcela);
		
		RegistroEmplacamentoAvisoLegal registroEmp = new RegistroEmplacamentoAvisoLegal();
		registroEmp = new RegistroEmplacamentoAvisoLegal("SP", new BigDecimal(1000));
		
		registroEmp.getUf();
		registroEmp.getValor();
		
		assertNotNull(registroEmp);
		
		CestaAvisoLegal cesta = new CestaAvisoLegal();
		cesta.setCodigo(1);
		cesta.setDescricao("Cesta");
		cesta.setValor(new BigDecimal(500));
		
		cesta.getCodigo();
		cesta.getDescricao();
		cesta.getValor();		
		
		cesta = new CestaAvisoLegal(cesta.getCodigo(), cesta.getDescricao(), cesta.getValor());
		
		assertNotNull(cesta);
		
		Plano plano = new Plano();
		
		VeiculoAvisoLegal veiculo = new VeiculoAvisoLegal();
		veiculo = new VeiculoAvisoLegal(modeloAvisoLegal, 2021, m, diaParcela, registroEmp, cesta, plano);
		
		veiculo.getAnoModelo();
		veiculo.getCesta();
		veiculo.getDiaParcela();
		veiculo.getMarca();
		veiculo.getModelo();
		veiculo.getPlano();
		veiculo.getRegistroEmplacamento();
		
		ParcelaAvisoLegal parcela = new ParcelaAvisoLegal();
		parcela = new ParcelaAvisoLegal(new BigDecimal(1000));
		
		parcela.getValorTaxaCadastro();
		
		assertNotNull(parcela);
		
		OrigemNegocioAvisoLegal origem = new OrigemNegocioAvisoLegal();
		origem = new OrigemNegocioAvisoLegal("CODIGO", "CNPJ", "SAO PAULO", "SAO PAULO");
		
		origem.getCidade();
		origem.getCnpj();
		origem.getCodigo();
		origem.getEstado();
		
		assertNotNull(origem);
		
		ResidualAvisoLegal residual = new ResidualAvisoLegal();
		residual = new ResidualAvisoLegal(new BigDecimal(1000));
		
		residual.getValor();
		
		assertNotNull(residual);
		
		PrazoAvisoLegal prazo = new PrazoAvisoLegal();
		prazo = new PrazoAvisoLegal(12, residual, "SVP", new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(1000));
		
		prazo.getNumeroParcelas();
		prazo.getResidual();
		prazo.getTipoSeguroPrestamista();
		prazo.getValorCetAnual();
		prazo.getValorIOF();
		prazo.getValorParcela();
		prazo.getValorSeguroPrestamista();
		prazo.getValorTaxaAnual();
		prazo.getValorTaxaMes();
		prazo.getValorTotalFinanciado();
		prazo.getValorTotalPrazo();
		
		assertNotNull(prazo);
		
		SimulacaoAvisoLegal simulacao = new SimulacaoAvisoLegal();
		simulacao = new SimulacaoAvisoLegal("Residual", prazo, null, new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(1000), new BigDecimal(1000), LocalDate.now(), parcela);
		
		simulacao.getDataBase();
		simulacao.getItens();
		simulacao.getParcela();
		simulacao.getPrazo();
		simulacao.getTipo();
		simulacao.getValorBem();
		simulacao.getValorEntrada();
		simulacao.getValorParcelaDesejada();
		simulacao.getValorResidual();
		
		assertNotNull(simulacao);
				
				
		PropostaAvisoLegal proposta = new PropostaAvisoLegal();
		proposta = new PropostaAvisoLegal(conf, veiculo, simulacao, "CNPJ", origem);
		
		proposta.getCnpjOrigemNegocio();
		proposta.getCodPlano();
		proposta.getConfiguracao();
		proposta.getOrigemNegocio();
		proposta.getSimulacao();
		proposta.getVeiculo();
		
		assertNotNull(proposta);
		
	}

}
