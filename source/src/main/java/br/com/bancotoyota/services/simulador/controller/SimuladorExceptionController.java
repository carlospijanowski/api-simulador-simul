package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.ErrorResponse;
import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoRequest;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.exceptions.*;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Luis Santos Implementação abstrata dos serviços
 */
@CrossOrigin("*")
@Slf4j
public abstract class SimuladorExceptionController {

	static final String JSON_MEDIA_TYPE = "application/json";
	public static final String TAXA_SUBSIDIO = "taxa-subsidio";
	public static final String MAXIMO_DE_REQUESTS_EM_PARALELO_ALCANCADO = "máximo de requests em paralelo alcançado";
	public static final String ERRO_INTERNO = "Erro interno";
	CalculadoraServices calculadoraServices;
	CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices;
	CalculadoraSeguroFranquiaServices seguroFranquiaServices;
	SimulacaoServices simulacaoServices;
	DataCarencia dataCarencia;
	ParametrosDeSeguroPrestamistaService parametrosDeSeguroPrestamistaService;
	protected boolean debugRetornarRequest;
	protected SeguroPrestamistaPlusServiceImpl prestamistaService;

	private static final Map<String, String> MENSAGENS_DE_ERRO;

	static {
		MENSAGENS_DE_ERRO = new HashMap<>();
		MENSAGENS_DE_ERRO.put("NotNull", "campo não informado");
	}

	public static final BigDecimal UM_CENTESIMO = new BigDecimal("0.01");

	/**
	 * usado para testes apenas
	 */
	@Getter(AccessLevel.PROTECTED)
	@Setter(AccessLevel.PROTECTED)
	private boolean aborted;

	public SimuladorExceptionController() {
		super();
	}

	public SimuladorExceptionController(CalculadoraServices calculadoraService, SimulacaoServices simulacaoService,
			DataCarencia dataCarencia, ParametrosDeSeguroPrestamistaService parametrosDeSeguroPrestamistaService,
			boolean debugRetornarRequest,SeguroPrestamistaPlusServiceImpl prestamistaService, CalculadoraSeguroFranquiaServices seguroFranquiaServices, CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices) {
		this.calculadoraServices = calculadoraService;
		this.simulacaoServices = simulacaoService;
		this.dataCarencia = dataCarencia;
		this.calculadoraSeguroMecanicaServices = calculadoraSeguroMecanicaServices;
		this.parametrosDeSeguroPrestamistaService = parametrosDeSeguroPrestamistaService;
		this.debugRetornarRequest = debugRetornarRequest;
		this.prestamistaService = prestamistaService;
		this.seguroFranquiaServices = seguroFranquiaServices;
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	@ResponseBody
	public ResponseEntity<List<Erro>> exceptionHandler(MissingServletRequestParameterException exception) {
		log.error("error MissingServletRequestParameterException", exception);

		Erro erro = new Erro(exception.getParameterName(), null, EnumErroValidacao.ERRO_CAMPO_OBRIGATORIO.getKey(),
				"campo não informado");
		return ResponseEntity.badRequest().body(Arrays.asList(erro));
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	@ResponseBody
	public ResponseEntity<List<Erro>> exceptionHandler(MissingRequestHeaderException exception) {
		log.error("error MissingServletRequestParameterException", exception);

		Erro erro = new Erro(exception.getHeaderName(), null, EnumErroValidacao.ERRO_CAMPO_OBRIGATORIO.getKey(),
				"header não informado");
		return ResponseEntity.badRequest().body(Arrays.asList(erro));
	}

	@ExceptionHandler(InvalidFieldException.class)
	@ResponseBody
	public ResponseEntity<List<Erro>> exceptionHandler(InvalidFieldException exception) {
		log.error("error InvalidFieldException", exception);

		return ResponseEntity.badRequest().body(exception.getErros());
	}

	@ExceptionHandler({ HttpMessageConversionException.class })
	@ResponseBody
	public ResponseEntity<List<Erro>> exceptionHandler(HttpMessageConversionException exception) {
		log.error("error HttpMessageConversionException", exception);
		Throwable cause = exception.getCause();
		if (cause instanceof InvalidFormatException) {
			InvalidFormatException ex = (InvalidFormatException) cause;
			return ResponseEntity.badRequest()
					.body(Arrays.asList(new Erro(getPath(ex), String.valueOf(ex.getValue()), "campo inválido")));
		}

		Erro erro = new Erro("Os dados enviados não estão no formato correto. Erro: "
				+ (cause != null ? cause.getMessage() : exception.toString()));
		return ResponseEntity.badRequest().body(Arrays.asList(erro));
	}

	private String getPath(InvalidFormatException ex) {
		StringBuilder stringBuilder = new StringBuilder();
		ex.getPath().forEach(p -> stringBuilder.append(p.getFieldName()).append("."));
		if (stringBuilder.charAt(stringBuilder.length() - 1) == '.') {
			stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		}
		return stringBuilder.toString();
	}

	@ExceptionHandler(BusinessValidationException.class)
	@ResponseBody
	public ResponseEntity<List<Erro>> handlerMethodArgumentNotValidException(BusinessValidationException ex) {
		String message = ex.getMessage();
		if (message.equals(MAXIMO_DE_REQUESTS_EM_PARALELO_ALCANCADO)) {
			log.error(message);
		} else {
			log.error("handlerMethodArgumentNotValidException", ex);
		}
		if (ex.getField() != null) {
			return ResponseEntity.badRequest().body(Arrays.asList(new Erro(ex.getField(), ex.getValue(),
					EnumErroValidacao.ERRO_CAMPO_OBRIGATORIO.getKey(), message)));
		} else {
			return ResponseEntity.badRequest().body(Arrays.asList(new Erro(message)));
		}
	}

	@ExceptionHandler({ InvalidTypeIdException.class })
	@ResponseBody
	public ResponseEntity<ErrorResponse> exceptionHandler(InvalidTypeIdException exception)
			throws JsonProcessingException {
		log.error("error InvalidTypeIdException", exception);
		Throwable cause = exception.getCause();
		ErrorResponse erro = new ErrorResponse(
				"Falha na Deserialização. Erro: " + (cause != null ? cause.getMessage() : exception.toString()));
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
	}

	@ExceptionHandler({ ServicoForaException.class })
	@ResponseBody
	public ResponseEntity<ErrorResponse> exceptionHandler(ServicoForaException exception)
			throws JsonProcessingException {
		log.error("error ServicoForaException", exception);
		ErrorResponse erro = new ErrorResponse(exception.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
	}

	@ExceptionHandler({ ThirdPartyException.class })
	@ResponseBody
	public ResponseEntity<ErrorResponse> exceptionHandler(ThirdPartyException exception)
			throws JsonProcessingException {
		log.error("error ThirdPartyException", exception);
		ErrorResponse erro = new ErrorResponse(exception.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
	}

	@ExceptionHandler(HttpClientErrorException.class)
	@ResponseBody
	public ResponseEntity<ErrorResponse> exceptionHandler(HttpClientErrorException exception) {
		log.error("error HttpClientErrorException", exception);
		log.error("HTTP response body: " + exception.getResponseBodyAsString());
		ErrorResponse erro;
		if (!StringUtils.isAllEmpty(exception.getMessage())) {
			erro = new ErrorResponse(exception.getMessage());
		} else {
			erro = new ErrorResponse(ERRO_INTERNO);
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
	}

	@ExceptionHandler(Throwable.class)
	@ResponseBody
	public ResponseEntity<ErrorResponse> exceptionHandler(Throwable exception) throws JsonProcessingException {
		if (exception instanceof ClientAbortException) {
			log.error("cliente abortou a conexão");
			// como o cliente não está mais esperando a resposta não faz sentido retornar
			// nada
			aborted = true;
			return null;
		} else {
			log.error("error Throwable", exception);
			ErrorResponse erro;
			if (!StringUtils.isAllEmpty(exception.getMessage())) {
				erro = new ErrorResponse(exception.getMessage());
			} else {
				String stackTrace = Objects.nonNull(exception.getStackTrace()) && exception.getStackTrace().length > 0 ? Arrays.stream(exception.getStackTrace()).findFirst().toString() : "Não disponível";
				String mensagemErroIterno = ERRO_INTERNO + " - Causa " + exception.getClass().getCanonicalName() + ": " + stackTrace;
				erro = new ErrorResponse(mensagemErroIterno);
			}
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
		}
	}

	@ExceptionHandler(JsonParseException.class)
	@ResponseBody
	public ResponseEntity<ErrorResponse> exceptionHandler(JsonParseException exception) throws JsonProcessingException {
		log.error("error JsonParseException", exception);
		Throwable cause = exception.getCause();
		ErrorResponse erro = new ErrorResponse(
				"Falha na conversão do JSON. Erro: " + (cause != null ? cause.getMessage() : exception.toString()));
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
	}

	@ExceptionHandler(NumberFormatException.class)
	@ResponseBody
	public ResponseEntity<ErrorResponse> exceptionHandler(NumberFormatException exception)
			throws JsonProcessingException {
		log.error("error NumberFormatException", exception);
		Throwable cause = exception.getCause();
		ErrorResponse erro = new ErrorResponse("Falha na formatação dos numeros. Erro: "
				+ (cause != null ? cause.getMessage() : exception.toString()));
		return ResponseEntity.badRequest().body(erro);
	}

	@ExceptionHandler(RedisConnectionFailureException.class)
	@ResponseBody
	public ResponseEntity<ErrorResponse> exceptionHandler(RedisConnectionFailureException exception)
			throws JsonProcessingException {
		log.error("error RedisConnectionFailureException", exception);
		Throwable cause = exception.getCause();
		ErrorResponse erro = new ErrorResponse("Falha de conexão no redis. Erro: "
				+ (cause != null ? cause.getLocalizedMessage() : exception.toString()));
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(erro);
	}

	@ExceptionHandler(InvalidDefinitionException.class)
	@ResponseBody
	public ResponseEntity<ErrorResponse> exceptionHandler(InvalidDefinitionException exception)
			throws JsonProcessingException {
		log.error("error InvalidDefinitionException", exception);
		Throwable cause = exception.getCause();
		ErrorResponse erro = new ErrorResponse(
				"Definição inválida do objeto. Erro: " + (cause != null ? cause.getMessage() : exception.toString()));
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
	}

	@ExceptionHandler(EntityNotFoundException.class)
	@ResponseBody
	public ResponseEntity<ErrorResponse> exceptionHandler(EntityNotFoundException exception) throws IOException {
		log.error("error EntityNotFoundException", exception);
		Throwable cause = exception.getCause();
		ErrorResponse erro = new ErrorResponse("Entidade não encontrada. Erro: "
				+ (cause != null ? cause.getLocalizedMessage() : exception.getErro().getMessage()));
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseBody
	public ResponseEntity<List<Erro>> handlerMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
		log.error("handlerMethodArgumentNotValidException", ex);
		List<Erro> erros = new ArrayList<>();
		List<FieldError> list = ex.getBindingResult().getFieldErrors();
		Class baseClass = ex.getBindingResult().getTarget().getClass();

		list.forEach(error -> {

			String fieldName = pathTraduzido(baseClass, error.getField());

			erros.add(new Erro(fieldName, "" + error.getRejectedValue(),
					EnumErroValidacao.ERRO_CAMPO_OBRIGATORIO.getKey(), getMensagemDeErro(error)));
		});

		List<ObjectError> globalErrors = ex.getBindingResult().getGlobalErrors();

		globalErrors.forEach(error -> erros.add(new Erro(error.getObjectName(), null, getMensagemDeErro(error))));

		return new ResponseEntity<>(erros, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(SeguroPrestamistaNotFoundException.class)
	@ResponseBody
	public ResponseEntity<ErrorResponse> exceptionHandler(SeguroPrestamistaNotFoundException exception) throws IOException {
		log.error("error SeguroPrestamistaNotFoundException", exception);
		Throwable cause = exception.getCause();
		ErrorResponse erro = new ErrorResponse("Array de Seguros não encontrada. Erro: "
				+ (cause != null ? cause.getLocalizedMessage() : exception.getMessage()));
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
	}

	protected String pathTraduzido(Class baseClass, String path) {
		String node;
		String resto;
		int i = path.indexOf('.');
		if (i >= 0) {
			node = path.substring(0, i);
			resto = path.substring(i + 1);
		} else {
			node = path;
			resto = null;
		}

		Field field = findField(baseClass, node);
		if (field != null) {
			JsonProperty a = field.getAnnotation(JsonProperty.class);
			String result = a != null ? a.value() : field.getName();
			if (resto != null) {
				result += "." + pathTraduzido(field.getType(), resto);
			}
			return result;
		} else {
			return path;
		}
	}

	private Field findField(Class baseClass, String fieldName) {
		if (baseClass == null) {
			return null;
		}
		try {
			return baseClass.getDeclaredField(fieldName);
		} catch (NoSuchFieldException e) {
			return findField(baseClass.getSuperclass(), fieldName);
		}
	}

	private String getMensagemDeErro(ObjectError error) {
		String mensagemDeErro = MENSAGENS_DE_ERRO.get(error.getCode());
		if (mensagemDeErro == null) {
			log.warn("código de erro não mapeado '" + error.getCode() + "', retornamdo mensagem padrão '"
					+ error.getDefaultMessage());
			mensagemDeErro = error.getDefaultMessage();
		}
		return mensagemDeErro;
	}

	protected Subsidio validarSubsidio(SimulacaoRequest request, Collection<Prazo> prazos, boolean corrigirMaximo) {

		Subsidio subsidio = simulacaoServices.getSubsidio(request.getPlanoId());
		if (subsidio != null && subsidio.getTipoSubsidio() == EnumTipoSubsidio.FIXO) {
			if (request.getTaxaSubsidio() != null) {
				throw new BusinessValidationException(TAXA_SUBSIDIO, request.getTaxaSubsidio().toString(),
						"plano selecionado é do tipo subsídio fixo");
			}
			// a taxa do base_plano_taxas pode estar fora dos limites, então vamos corrigir,
			// mesmo porque a taxa
			// nem é passada
			corrigirMaximo = true;
			request.setTaxaSubsidio(subsidio.getTaxaBanco());
		}

		// A classe ServicesSimulacao (que não é parte do projeto) só usa a taxa ou
		// valor do subsídio se o código
		// de retorno for "S", o que me soa estranho. Além disso o valor do retorno
		// passado para a planilha é zero.
		// O correto seria passar null nesses dois campos caso não haja subsídio e nesse
		// caso setar o valor do
		// retorno para zero.
		if (request.getTaxaSubsidio() != null) {
			if (subsidio == null) {
				throw new BusinessValidationException(TAXA_SUBSIDIO, request.getTaxaSubsidio().toString(),
						"plano selecionado não possui subsídio");
			}
			request.setRetorno("S");
			BigDecimal maxTaxaSubsidio = getMaxTaxaSubsidio(prazos);
			if (!corrigirMaximo && maxTaxaSubsidio.compareTo(request.getTaxaSubsidio()) < 0
					|| request.getTaxaSubsidio().compareTo(BigDecimal.ZERO) < 0) {
				throw new BusinessValidationException(TAXA_SUBSIDIO, request.getTaxaSubsidio().toString(),
						"A taxa máxima para subsídio é de " + maxTaxaSubsidio.setScale(2, RoundingMode.HALF_UP) + " %");
			}
		}
		return subsidio;
	}

	/**
	 * Retorna a maior taxa banco menos 0.01, que é o limite que ficou combinado
	 * para o valor do subsídio que nunca pode ser igual ou maior que taxa banco o
	 * que significaria que o valor do subsídio seria zero ou negativo o que também
	 * não é permitido.
	 */
	protected BigDecimal getMaxTaxaSubsidio(Collection<Prazo> prazos) {
		return prazos.stream().map(Prazo::getTaxaBanco).max(Comparator.naturalOrder())
				.orElseThrow(() -> new RuntimeException(
						"Nunca vai passar aqui, mas temos que colocar esse orElseThrow pra calar a boca do Sonar"))
				.subtract(UM_CENTESIMO);
		// o motivo é simples, no método que carrega a lista de prazos já fazemos a
		// validação pra garantir que não é uma lista vazia
	}

	/**
	 * @param token   se for null não faz nada, isso indica que não deve ser
	 *                carregado
	 * @param request
	 */
	protected void carregarDadosDoProponente(String token, SimulacaoRequest request) {
		TipoPessoa tipoPessoa = request.getTipoPessoaEnum();
		if (token != null && tipoPessoa == TipoPessoa.PESSOA_FISICA) {
			ParametrosDeSeguroPrestamista parametros = parametrosDeSeguroPrestamistaService.getParametros(
					request.getOrigemNegocio(), request.getCpfProponente(), token,
					request.getParametrosDeSeguroPrestamista(), null);
			request.setParametrosDeSeguroPrestamista(parametros);
			request.setDataNascimento(parametrosDeSeguroPrestamistaService
					.getDataNascimento(request.getDataNascimento(), request.getCpfProponente(), token, null));
		}
	}

	protected <T> T getDebugRequest(T request) {
		if (debugRetornarRequest) {
			return request;
		} else {
			return null;
		}
	}

	private Lock lock = new ReentrantLock(true);

	private ThreadLocal<Boolean> isRequestExterno = new ThreadLocal<>();

	private int requestsExternos = 0;

	protected void finalizarRequest() {
		lock.lock();
		try {
			Boolean value = isRequestExterno.get();
			if (value != null && value.booleanValue()) {
				requestsExternos--;
				isRequestExterno.remove();
			}
		} finally {
			lock.unlock();
		}
	}

	protected void limitarRequestsExternos(List<String> list, int maxRequestsExternos) {
		if (list != null && !list.isEmpty() && maxRequestsExternos != 0) {
			String forwarded = list.get(0);
			if (forwarded.indexOf(',') < 0) {
				// O request não vem do front, desta forma vamos limitar o número de threads.
				// Os requests to front passam pelo proxy do front e por isso todos tem uma
				// virgula para separar as duas passagens.
				lock.lock();
				try {
					if (requestsExternos >= maxRequestsExternos) {
						throw new BusinessValidationException(MAXIMO_DE_REQUESTS_EM_PARALELO_ALCANCADO);
					}
					isRequestExterno.set(true);
					requestsExternos++;
				} finally {
					lock.unlock();
				}
			}
		}
	}

	// forwarded=[for=10.102.1.6;host=api-financiamentos-simul-new-front-homolog.containers.sistemas.devbtb;proto=http;proto-version=]
	// forwarded=[for=172.16.200.129;host=app-new-front-homolog.containers.sistemas.devbtb;proto=http;proto-version=,
	// for=10.102.1.6;host=api-financiamentos-simul-new-front-homolog.containers.sistemas.devbtb;proto=http;proto-version=]

	public LocalDate getDataCalculo(SimulacaoRequest request) {
		if (request.getDataCalculo() != null) {
			return request.getDataCalculo();
		} else {
			return dataCarencia.getDataCalculo();
		}
	}
}
