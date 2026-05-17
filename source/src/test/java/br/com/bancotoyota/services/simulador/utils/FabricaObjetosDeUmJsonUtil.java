package br.com.bancotoyota.services.simulador.utils;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FabricaObjetosDeUmJsonUtil {
	public  <T> T criarObjetoDoJson(String arquivo, Class<T> clazz)
			throws IOException, JsonProcessingException, JsonMappingException {
		InputStream ref = this.getClass().getClassLoader().getResourceAsStream(arquivo);
		JsonNode jn = new ObjectMapper().readTree(ref);
		ObjectMapper om = new ObjectMapper();
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		om.findAndRegisterModules();
		T objectFromJson = (T) om.readValue(jn.toString(), clazz);

		ref.close();
		return objectFromJson;
	}
}
