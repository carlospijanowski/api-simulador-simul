package br.com.bancotoyota.services.simulador.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import br.com.bancotoyota.services.simulador.entities.CestaAvisoLegal;
import br.com.bancotoyota.services.simulador.entities.ConfiguracaoAvisoLegal;
import br.com.bancotoyota.services.simulador.entities.DiaParcela;
import br.com.bancotoyota.services.simulador.entities.MarcaAvisoLegal;
import br.com.bancotoyota.services.simulador.entities.ModeloAvisoLegal;
import br.com.bancotoyota.services.simulador.entities.OrigemNegocioAvisoLegal;
import br.com.bancotoyota.services.simulador.entities.ParcelaAvisoLegal;
import br.com.bancotoyota.services.simulador.entities.PrazoAvisoLegal;
import br.com.bancotoyota.services.simulador.entities.PrazoSimulacaoSimplificada;
import br.com.bancotoyota.services.simulador.entities.PropostaAvisoLegal;
import br.com.bancotoyota.services.simulador.entities.PropostaTextoAvisoLegalResponse;
import br.com.bancotoyota.services.simulador.entities.RegistroEmplacamentoAvisoLegal;
import br.com.bancotoyota.services.simulador.entities.ResidualAvisoLegal;
import br.com.bancotoyota.services.simulador.entities.SimulacaoAvisoLegal;
import br.com.bancotoyota.services.simulador.entities.SimulacaoSimplificada;
import br.com.bancotoyota.services.simulador.entities.TipoSimulacao;
import br.com.bancotoyota.services.simulador.entities.VeiculoAvisoLegal;
import br.com.bancotoyota.services.simulador.services.PropostasFinanciamentoService;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PropostasFinanciamentoServiceImpl implements PropostasFinanciamentoService{

	public static final String ERRO_NA_API_DE_PROPOSTAS_DE_FINANCIAMENTO = "Erro na API de propostas de financiamento";
    private RestTemplate restTemplate;
    private String apiPropostasFinanciamentoUrl;	
	
    @Autowired
    public PropostasFinanciamentoServiceImpl(RestTemplate restTemplate, @Value("${api.propostas-financiamento.url:}") String apiPropostasFinanciamentoUrl) {
		this.restTemplate = restTemplate;
		this.apiPropostasFinanciamentoUrl = apiPropostasFinanciamentoUrl;
	}
    
	@Override
	public String gerarTextoAvisoLegal(PropostaAvisoLegal proposta) {
		log.info("Obtendo texto de aviso legal {}...", proposta.getCnpjOrigemNegocio());
		
		try {
			
			String url = apiPropostasFinanciamentoUrl.concat("/aviso-legal");
			
			HttpHeaders requestHeaders = new HttpHeaders();
			requestHeaders.add("Accept", MediaType.APPLICATION_JSON_VALUE);
			
			HttpEntity<PropostaAvisoLegal> httpEntity = new HttpEntity<>(proposta,requestHeaders);
			ResponseEntity<PropostaTextoAvisoLegalResponse> responseEntity = restTemplate.exchange(url, HttpMethod.POST, httpEntity,PropostaTextoAvisoLegalResponse.class);
			return responseEntity.getBody().getAvisoLegal();
			
		}catch (HttpStatusCodeException e) {
			throw new ThirdPartyException(ERRO_NA_API_DE_PROPOSTAS_DE_FINANCIAMENTO, e);
		}
	}

	@Override
	public String gerarTextoAvisoLegalSimulacaoSimplificada(SimulacaoSimplificada simulacaoSimplificada, Integer prazoDesejado) {
		
		PropostaAvisoLegal proposta = new PropostaAvisoLegal();
		
		//Gera o texto somente de simulações com data de primeiro vencimento de 1 mês após a formalização
		if ( simulacaoSimplificada.getEntrada().getDetalheSimulacao().getDataPrimeiroVencimento() != null ) {
			throw new BusinessValidationException("Texto de aviso legal só é gerado para simulações com data do primeiro vencimento default");
		}
		
		proposta.setCnpjOrigemNegocio(simulacaoSimplificada.getEntrada().getDealer().getCnpjOrigemNegocio());
		proposta.setVeiculo(preencherDadosVeiculo(simulacaoSimplificada));
		proposta.setConfiguracao(preencherConfiguracao(simulacaoSimplificada));
		proposta.setOrigemNegocio(preencherOrigemNegocio(simulacaoSimplificada));
		proposta.setSimulacao(preencherSimulacaoAvisoLegal(simulacaoSimplificada, prazoDesejado));
		
		
		return gerarTextoAvisoLegal(proposta);
	}
	
	private CestaAvisoLegal preencherCestaServicos( SimulacaoSimplificada simulacaoSimplificada ) {
		CestaAvisoLegal cesta = new CestaAvisoLegal();
		if (simulacaoSimplificada.getSaida().getCestaServico() != null) {
			cesta.setCodigo(simulacaoSimplificada.getSaida().getCestaServico().getCodigo());
			cesta.setDescricao(simulacaoSimplificada.getSaida().getCestaServico().getDescricao());
			cesta.setValor(simulacaoSimplificada.getSaida().getCestaServico().getValor());
		}
		return cesta;
	}
	
	private MarcaAvisoLegal preencherMarca( SimulacaoSimplificada simulacaoSimplificada ) {
		MarcaAvisoLegal marca = new MarcaAvisoLegal();
		marca.setId(simulacaoSimplificada.getEntrada().getVeiculo().getMarcaId());
		marca.setDescricao(simulacaoSimplificada.getEntrada().getVeiculo().getMarcaDescricao());
		return marca;
	}

	private ModeloAvisoLegal preencherModelo( SimulacaoSimplificada simulacaoSimplificada ) {
		ModeloAvisoLegal modelo = new ModeloAvisoLegal();
		modelo.setAnoModelo(simulacaoSimplificada.getEntrada().getVeiculo().getAnoModelo());
		modelo.setCodigoModelo(simulacaoSimplificada.getEntrada().getVeiculo().getModeloId());
		modelo.setCodigoMolicar(simulacaoSimplificada.getEntrada().getVeiculo().getCodigoMolicar());
		modelo.setDescricaoModelo(simulacaoSimplificada.getEntrada().getVeiculo().getModeloDescricao());
		return modelo;
	}
	
	private RegistroEmplacamentoAvisoLegal preencherRegistroEmplacamento( SimulacaoSimplificada simulacaoSimplificada ) {
		RegistroEmplacamentoAvisoLegal registroEmplacamento = new RegistroEmplacamentoAvisoLegal();
		registroEmplacamento.setUf(simulacaoSimplificada.getSaida().getEmplacamento().getUf());
		registroEmplacamento.setValor(simulacaoSimplificada.getSaida().getEmplacamento().getValor());
		return registroEmplacamento;
	}
	
	private VeiculoAvisoLegal preencherDadosVeiculo( SimulacaoSimplificada simulacaoSimplificada ) {
		VeiculoAvisoLegal veiculo = new VeiculoAvisoLegal();
		veiculo.setDiaParcela(new DiaParcela().setDescricao("1 mês após a formalização"));
		veiculo.setCesta(preencherCestaServicos(simulacaoSimplificada));
		veiculo.setMarca(preencherMarca(simulacaoSimplificada));
		veiculo.setModelo(preencherModelo(simulacaoSimplificada));
		veiculo.setAnoModelo(simulacaoSimplificada.getEntrada().getDetalheSimulacao().getAnoModelo());
		veiculo.setRegistroEmplacamento(preencherRegistroEmplacamento(simulacaoSimplificada));
		veiculo.setPlano(simulacaoSimplificada.getSaida().getPlano());
		return veiculo;
	}
	
	private ConfiguracaoAvisoLegal preencherConfiguracao( SimulacaoSimplificada simulacaoSimplificada ) {
		ConfiguracaoAvisoLegal configuracao = new ConfiguracaoAvisoLegal();
		configuracao.setDataCalculo(simulacaoSimplificada.getDataCriacao().toLocalDate());
		configuracao.setModalidade(simulacaoSimplificada.getEntrada().getDetalheSimulacao().getModalidadeId().toLowerCase());
		configuracao.setSituacaoVeiculo(simulacaoSimplificada.getEntrada().getDetalheSimulacao().getSituacaoVeiculo());
		configuracao.setTipoFinanciamento(simulacaoSimplificada.getEntrada().getDetalheSimulacao().getModalidadeId().toLowerCase());
		configuracao.setTipoPessoa(simulacaoSimplificada.getEntrada().getDetalheSimulacao().getTipoPessoa().toLowerCase());
		return configuracao;
	}
	
	private OrigemNegocioAvisoLegal preencherOrigemNegocio ( SimulacaoSimplificada simulacaoSimplificada ) {
		OrigemNegocioAvisoLegal origemNegocio = new OrigemNegocioAvisoLegal();
		origemNegocio.setCnpj(simulacaoSimplificada.getEntrada().getDealer().getCnpjOrigemNegocio());
		origemNegocio.setCidade(simulacaoSimplificada.getEntrada().getDealer().getCidade());
		origemNegocio.setCodigo(simulacaoSimplificada.getEntrada().getDealer().getCodigoLoja());
		origemNegocio.setEstado(simulacaoSimplificada.getEntrada().getDealer().getEstado());
		return origemNegocio;
	}
	
	private SimulacaoAvisoLegal preencherSimulacaoAvisoLegal( SimulacaoSimplificada simulacaoSimplificada, Integer prazoDesejado ) {
		SimulacaoAvisoLegal simulacao = new SimulacaoAvisoLegal();
		simulacao.setDataBase(simulacaoSimplificada.getSaida().getDataBase());
		simulacao.setValorBem(simulacaoSimplificada.getSaida().getSimulacao().getValorBem());
		simulacao.setValorEntrada(simulacaoSimplificada.getSaida().getSimulacao().getValorEntrada());
		simulacao.setValorParcelaDesejada(simulacaoSimplificada.getEntrada().getDetalheSimulacao().getValorParcelaDesejada());
		simulacao.setValorResidual(simulacaoSimplificada.getEntrada().getDetalheSimulacao().getValorParcelaResidual());
		
		// desejada, residual ou prazo
		if (simulacaoSimplificada.getTipoSimulacao().equals(TipoSimulacao.SIMPLIFICADA_DESEJADA)) {
			simulacao.setTipo("desejada");
		} else if (simulacaoSimplificada.getTipoSimulacao().equals(TipoSimulacao.SIMPLIFICADA_RESIDUAL)) {
			simulacao.setTipo("residual");
		}
		simulacao.setParcela(new ParcelaAvisoLegal(simulacaoSimplificada.getSaida().getSimulacao().getValorTarifaCadastro()));
		simulacao.setItens(simulacaoSimplificada.getEntrada().getItensFinanciaveis());
		
		simulacao.setPrazo(preencherPrazoAvisoLegal(simulacaoSimplificada, prazoDesejado));
		return simulacao;
	}
	
	private PrazoAvisoLegal preencherPrazoAvisoLegal( SimulacaoSimplificada simulacaoSimplificada, Integer prazoDesejado ) {
		PrazoSimulacaoSimplificada prazoSimplificada = simulacaoSimplificada.getSaida().getSimulacao().getPrazos().stream().filter(
				p -> p.getParcelas().equals(prazoDesejado)).findFirst().orElseThrow(() -> new EntityNotFoundException(PrazoSimulacaoSimplificada.class, "prazo-desejado"));
		
		PrazoAvisoLegal prazo = new PrazoAvisoLegal();
		prazo.setNumeroParcelas(prazoSimplificada.getParcelas());
		prazo.setResidual(new ResidualAvisoLegal(prazoSimplificada.getValorParcelaResidual()));
		if (prazoSimplificada.getTipoDeSeguroPrestamista() != null) {
			prazo.setTipoSeguroPrestamista(prazoSimplificada.getTipoDeSeguroPrestamista().toString());
		}
		prazo.setValorCetAnual(prazoSimplificada.getTaxaCetAnual());
		prazo.setValorIOF(prazoSimplificada.getValorIOF());
		prazo.setValorParcela(prazoSimplificada.getValorParcela());
		prazo.setValorSeguroPrestamista(prazoSimplificada.getValorDoSeguroPrestamista());
		prazo.setValorTaxaAnual(prazoSimplificada.getTaxaAnual());
		prazo.setValorTaxaMes(prazoSimplificada.getTaxaMensal());
		prazo.setValorTotalFinanciado(prazoSimplificada.getValorTotalFinanciado());
		prazo.setValorTotalPrazo(prazoSimplificada.getValorTotalPrazo());
		return prazo;
	}

}
