package br.com.bancotoyota.services.simulador.services.impl;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcDesejadaResponse;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcResidualResponse;
import br.com.bancotoyota.services.simulador.services.ConvertSimplificadaService;
import lombok.Setter;

@Service
public class ConvertSimplificadaServiceImpl implements ConvertSimplificadaService{
	
	
	@Override
	public SimulacaoParcDesejadaResponse convertResponseResidualToDesejada(
			ResponseEntity<SimulacaoParcResidualResponse> responseResidual) {		
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.findAndRegisterModules();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			String json = mapper.writeValueAsString(responseResidual.getBody());
			return mapper.readValue(json, SimulacaoParcDesejadaResponse.class);
		} catch (IOException ex) {
			throw new RuntimeException("erro convertendo retorno", ex);
		}
			
	}

	
	
}
