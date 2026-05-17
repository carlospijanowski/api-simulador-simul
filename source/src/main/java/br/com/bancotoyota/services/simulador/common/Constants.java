package br.com.bancotoyota.services.simulador.common;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;

public class Constants {

    private Constants() {}

	public static final String ERRO_TIPO_SEGURO_PRESTAMISTA_INVALIDO = "tipo de seguro prestamista deve ser %s";
	public static final String ERRO_SEGURO_PRESTAMISTA_INVALIDO = "valor do seguro prestamista inválido";
	public static final String ERRO_PLANO_SEM_SUBSIDIO_TAXA_MENSAL_INVALIDA = "taxa mensal deve ser %s ";
	public static final String ERRO_PLANO_SEM_SUBSIDIO_PLANO_NAO_ACEITA_SUBSIDIO = "plano não aceita subsídio";
	public static final String ERRO_PLANO_NAO_ENCONTRADO = "plano não encontrado";
	public static final String ERRO_PERCENTUAL_RESIDUAL_SUPERIOR_LIMITE = "percentual do residual superior ao limite máximo de %S";
	public static final String ERRO_PERCENTUAL_RESIDUAL_INFERIOR_LIMITE = "percentual do residual inferior ao limite mínimo de %s";
	public static final String ERRO_PERCENTUAL_BALAO_SUPERIOR_LIMITE = "Percentual da intermediaria superior ao limite máximo de %S";
	public static final String ERRO_PERCENTUAL_BALAO_INFERIOR_LIMITE = "Percentual da intermediaria inferior ao limite mínimo de %s";
	public static final String ERRO_API_CLIENTES_INDISPONIVEL = "não foi possível carregar dados do proponente";
	public static final String ERRO_PLANO_COM_SUBSIDIO_SEM_TAXA = "taxa de subsídio não informada para plano com subsídio";
	public static final String ERRO_PLANO_COM_SUBSIDIO_TAXA_INVALIDA = "esse plano com subsídio fixo deve usar taxa %s";
	public static final String ERRO_PLANO_COM_SUBSIDIO_TAXA_MINIMA_ERRADA = "o mínimo para a taxa de subsídio é zero";
	public static final String ERRO_PLANO_COM_SUBSIDIO_TAXA_MAXIMA_EXCEDIDA = "o máximo para a taxa de subsídio é %s";
	public static final String ERRO_PLANO_COM_SUBSIDIO_TAXA_MES_IGUAL_TAXA_SUBSIDIO = "para planos com subsídio a taxa ao mês deve ser igual a taxa do subsídio";
	public static final String ERRO_PERCENTUAL_ENTRADA_FORA_LIMITE_PLANO = "percentual de entrada não se enquadra nos limites do plano";
	public static final String ERRO_PLANO_NAO_PODE_SER_USADO_COM_DETERMINADA_QUANTIDADE_PARCELAS = "plano não pode ser usado com esse número de parcelas";
	public static final String KEY_VALIDACAO_BASICA = "validacao-basica";
	public static final String KEY_PRE_VALIDACAO_OFERTA = "pre-validacao-oferta";
	public static final String KEY_VALIDACAO_COMPLETA = "validacao-completa";
	public static final String RETORNO_DEFAULT = "1";
	public static final int ZERO_INTEIRO =0;
	public static final String FORMATO_DATA_BRASIL = "dd/MM/yyyy";
	public static final BigDecimal UM_CENTESIMO_PORCENTO = new BigDecimal("0.01").divide(new BigDecimal(100));
	public static final int QUTDE_PARCELAS = 60;

	/**
	 * Usado em um teste e na geração do CDS
	 */
	public static final String JSON_SIMULACAO = "{\n" +
			"    \"plano-id\": -1,\n" +
			"    \"taxa-mes\": 1.5,\n" +
			"    \"permite-balao\":true,\n" +
			"    \"data-calculo\": \"20190304\",\n" +
			"    \"isenta-iof\": \"S\",\n" +
			"\n" +
			"    \"parcelas\": [14],\n" +
			"\n" +
			"    \"valor-parcela-residual\": null,\n" +
			"\n" +
			"\t\"modalidade\":\"cdc\",\n" +
			"    \"valor-taxa-cadastro\": 550,\n" +
			"    \"valor-cesta-servicos\": 500,\n" +
			"    \"vencimento-n-dias\": true,\n" +
			"    \"vencimento-30-dias\": true,\n" +
			"    \"periodicidade\": 1,\n" +
			"    \"parametros-seguro-prestamista\": {\n" +
			"        \"spf-removido\": true,\n" +
			"        \"svp-removido\": false,\n" +
			"        \"spf-disponivel\": false,\n" +
			"        \"svp-disponivel\": true,\n" +
			"        \"svp-se-spf-removido\": true,\n" +
			"        \"spf-valor-comprometido\": 196742.87,\n" +
			"        \"svp-valor-comprometido\": 1073640.91\n" +
			"    },\n" +
			"    \"tipo-pessoa\": \"pessoa-fisica\",\n" +
			"    \"retorno\": \"0\",\n" +
			"    \"carencia-do-plano\": {\n" +
			"        \"minimo\": 10,\n" +
			"        \"maximo\": 180\n" +
			"    },\n" +
			"    \"valor-uf-emplacamento\": 366.19,\n" +
			"    \"valor-bem\": 120000,\n" +
			"    \"valor-entrada\": 80000,\n" +
			"    \"valor-parcela-desejada\": 3000,\n" +
			"    \"data-nascimento\": \"19890919\",\n" +
			"    \"itens-financiaveis\": [],\n" +
			"    \"seguro-auto\": null\n" +
			"}";
	public static final long UM = 1;
	@Value("${spring.retry.numero-retentativas}")
	public static final int NUMERO_RETENTATIVAS=5;
	@Value("${spring.retry.delay-retentativas-ms}")
	public static final int DELAY_RETENTATIVAS=1000;
	public static final String ERRO_API_CLIENTES = "Erro na API de clientes";
	public static final String ERRO_API_SEGURO_GARANTIA= "Erro na API de seguro garantia";
	public static final String VERDADEIRO = "VERDADEIRO";
	public static final String FALSO = "FALSO";
	public static final String ZERO_STRING = "0";
	public static final String PARCELAS = "parcelas";
	
}