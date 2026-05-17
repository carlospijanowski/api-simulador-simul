package br.com.bancotoyota.services.simulador.services.impl;

import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import br.com.bancotoyota.services.simulador.entities.ModeloVeiculos;
import br.com.bancotoyota.services.simulador.entities.VeiculosResponse;
import br.com.bancotoyota.services.simulador.services.ApiService;
import br.com.bancotoyota.services.simulador.services.VeiculosService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class VeiculosServiceImpl implements VeiculosService{

	
    private RestTemplate restTemplate;
    private ApiService apiService;
    private String apiVeiculosUrl;
    
    @Autowired
    public VeiculosServiceImpl(RestTemplate restTemplate, ApiService apiService,
                                   @Value("${api.veiculos.url:}") String apiVeiculosUrl) {
        this.restTemplate = restTemplate;
        this.apiVeiculosUrl = apiVeiculosUrl;
        this.apiService = apiService;
    }	   	
	
	@Override
	public ModeloVeiculos getModelo(String marcaId, String modeloId, Integer anoModelo, String token) {
		log.info("Obtendo modelo de veículo da marca {}...", marcaId);
		String url = apiVeiculosUrl.concat("/marcas/{marcaId}/modelos");
		
		try {
			Map<String, String> pathParams = new HashMap<>();
			pathParams.put("marcaId",marcaId);
			
			/*
			 * Quando o ano Modelo não for enviado, a primeira busca dos dados do veículo deve ser realizada com o anoModelo 9999 (veículo 0km)
			*/
			if (anoModelo == null) {
				anoModelo = 9999;
			}
			String anoModeloAux = anoModelo.toString();
			
			UriComponents urlBuilder = UriComponentsBuilder.fromHttpUrl(url).buildAndExpand(pathParams);
			
			log.info("Endpoint: {}", urlBuilder.toUriString());
					
			ResponseEntity<VeiculosResponse> responseEntity = restTemplate.exchange(urlBuilder.toUriString(), HttpMethod.GET,
                    new HttpEntity<>(apiService.getAuthorizedHeader(token)), VeiculosResponse.class);
			
			List<ModeloVeiculos> modelosMarca = responseEntity.getBody().getResponse().getModelos();
			
			//os modelos do veiculo pela marcaId e modeloId
			List<ModeloVeiculos> modelos = modelosMarca.stream().filter(m -> m.getCodigoModelo().equals(modeloId) && 
					m.getMarca() != null && m.getMarca().getId().equals(marcaId)).collect(Collectors.toList());
			
			Integer anoAtual = LocalDate.now().get(ChronoField.YEAR);
			//Busca o modelo no ano enviado como parâmetro, caso não encontre, tenta obter o modelo do ano atual, caso não encontre, pega o primeiro modelo que estiver na lista.
			return           modelos.stream().filter(m -> m.getAnoModelo() != null && m.getAnoModelo().equals(anoModeloAux)).findFirst()
					.orElse( modelos.stream().filter(m -> m.getAnoModelo() != null && m.getAnoModelo().equals(anoAtual.toString())).findFirst()
					.orElse( modelos.stream().findFirst().orElseThrow(() -> new EntityNotFoundException(ModeloVeiculos.class, "modelo-veiculo"))));
        } catch (HttpStatusCodeException e) {
            throw new ThirdPartyException("Erro na API de Veiculos", e);
        }		
	}

	public ModeloVeiculos getModeloZeroKm(String marcaId, String modeloId, Integer anoModeloZeroKm, String token) {
		//Quando é zero km ele faz a busca do modelo para ano-modelo 9999
		return getModelo(marcaId, modeloId, null, token);
	}
	
}