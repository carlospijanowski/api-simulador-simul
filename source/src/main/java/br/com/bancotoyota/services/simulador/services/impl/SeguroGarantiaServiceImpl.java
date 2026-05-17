package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.entities.ComboSeguroGarantiaResponse;
import br.com.bancotoyota.services.simulador.entities.EnumErroValidacao;
import br.com.bancotoyota.services.simulador.entities.SeguroGarantiaRequest;
import br.com.bancotoyota.services.simulador.services.ApiService;
import br.com.bancotoyota.services.simulador.services.SeguroGarantiaService;
import br.com.bancotoyota.services.simulador.services.TokenService;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.services.exceptions.ServicoForaException;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class SeguroGarantiaServiceImpl implements SeguroGarantiaService {

    private RestTemplate restTemplate;
    private ApiService apiService;
    private String apiSeguroGarantiaUrl;
    private TokenService tokenService;

    @Autowired
    public SeguroGarantiaServiceImpl(RestTemplate restTemplate, ApiService apiService,
                                     @Value("${api.seguro.garantia.url}") String apiSeguroGarantiaUrl, TokenService tokenService) {
        this.restTemplate = restTemplate;
        this.apiSeguroGarantiaUrl = apiSeguroGarantiaUrl;
        this.apiService = apiService;
        this.tokenService = tokenService;
    }

    @Override
    public ComboSeguroGarantiaResponse getValor(String cnpjOrigemNegocio, SeguroGarantiaRequest request) {
        log.info("Obtendo os valores de seguro garantia {} {}...", cnpjOrigemNegocio, request);
        try {
            String url = apiSeguroGarantiaUrl + cnpjOrigemNegocio;

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("marca", URLEncoder.encode(request.getMarca(), StandardCharsets.UTF_8))
                    .queryParam("modelo", URLEncoder.encode(request.getModelo(), StandardCharsets.UTF_8))
                    .queryParam("codigo-fipe", request.getCodigoFipe())
                    .queryParam("possui-garantia", request.getPossuiGarantia())
                    .queryParam("quilometragem", request.getQuilometragem())
                    .queryParam("ano-fabricacao-veiculo", request.getAnoFabricacao())
                    .queryParam("ano-modelo-veiculo", request.getAnoModelo());

            if (request.getDataInicioGarantia() != null) {
                builder.queryParam("data-inicio-garantia", request.getDataInicioGarantia());
            }

            log.info("URL final gerada: {}", builder.toUriString());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + tokenService.generateDirectAccessToken().getToken());
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<ComboSeguroGarantiaResponse> responseEntity = restTemplate.exchange(
                    builder.toUriString(), HttpMethod.GET, entity, ComboSeguroGarantiaResponse.class
            );

            log.info("Consulta de cotações do seguro garantia {}", responseEntity.getBody());

            return responseEntity.getBody();

        } catch (HttpClientErrorException.NotAcceptable exception) {
            log.error("Seguro Mecânica não elegível para o cálculo - Status Code: " + exception.getStatusCode(), exception);
            throw new BusinessValidationException(EnumErroValidacao.ERRO_SEGURO_MECANICA_INELEGIVEL.getKey(), EnumErroValidacao.ERRO_SEGURO_MECANICA_INELEGIVEL.getValue());
        } catch (HttpServerErrorException ex) {
            log.error("Erro ao consultar a api seguro garantia - Status Code: " + ex.getStatusCode(), ex);

            if (ex.getStatusCode().value() >= 500) {
                throw new ServicoForaException("O serviço está fora do ar: " + ex.getStatusCode() + " - " + ex.getMessage(), ex);
            }
            throw new ThirdPartyException(Constants.ERRO_API_SEGURO_GARANTIA, ex);
        } catch (RestClientException e) {
            log.error("Erro ao consultar a api de seguro garantia", e);
            throw new ThirdPartyException(Constants.ERRO_API_SEGURO_GARANTIA, e);
        }
    }

}
