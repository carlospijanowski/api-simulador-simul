package br.com.bancotoyota.services.simulador.beans.response;

public class Constants {

    //Mensagem de erro
    public static final String ERRO_CAMPO_INVALIDO = "Valor do campo %s inválido";
    public static final String ERRO_NOT_IMPLEMENTED = "Não existe recurso implementado para esta rota ou operação.";
    public static final String ERRO_INTERNAL_SERVER_ERROR = "Internal Error";
    public static final String ERRO_OBJETO_NAO_ENCONTRADO = "%s não encontrado(a)!";
    public static final String ERRO_PARAMETRO_OBRIGATORIO = "Campo %s deve ser informado";
    public static final String ERRO_HEADER_OBRIGATORIO = "Header %s deve ser informado";

    //Key Redis
    public static final String KEY_VENDA_DIRETA_SIM = "venda-direta-sim";
    public static final String KEY_VENDA_DIRETA_NAO = "venda-direta-nao";
	public static final String KEY_CDC = "cdc";
    public static final String KEY_LEASING = "leasing";
    public static final String KEY_CICLO_TOYOTA = "ciclo-toyota";
    public static final String KEY_ZERO_KM = "0km";
    public static final String KEY_USADO = "n0km";
    public static final String KEY_COM_SUBSIDIO = "com-subsidio";
    public static final String KEY_SEM_SUBSIDIO = "sem-subsidio";
    public static final String KEY_PESSOA_FISICA = "pf";
    public static final String KEY_PESSOA_JURIDICA = "pj";
    
    public static final int ZERO = 0;
    public static final String ORIGEM_NEGOCIO_LABEL = "origem-negocio";
    public static final String ORIGEM_NEGOCIO_DADOS_USUARIO_LABEL="dados-usuario";
	public static final String TRUE = "true";
	public static final String ERRO_PLANO_BLOQUEADO = "plano bloqueado";
	public static final String ERRO_PLANO_EXPIRADO = "plano expirado";
	public static final String ERRO_PLANO_NAO_COMPATIVEL_TIPO_PESSOA = "plano não compatível com tipo de pessoa";
	public static final String ERRO_PLANO_SO_PODE_SER_USADO_COM_SUBSIDIO = "plano só pode ser usado com subsídio";
	public static final String ERRO_MODELO_NAO_COMPATIVEL_PLANO = "modelo/marca não compatível com plano";
	public static final String ERRO_ANO_MODELO_SITUACAO_NAO_COMPATIVEL = "ano modelo/situação do veículo não compatível com o plano";
	public static final String ERRO_PLANO_NAO_PERMITIDO_LOJA = "plano não permitido para cnpj loja";
	public static final String ERRO_PERFIL_NAO_PERMITIDO_PLANO = "perfil não permitido para plano";
	public static final String ERRO_PLANO_NAO_DISPONIVEL_REGIAO = "plano não disponível para região";
	public static final String ERRO_CODIGO_RETORNO_INVALIDO = "código de retorno inválido";
	public static final String ERRO_CODIGO_RETORNO_NAO_ENCONTRADO = "códigos de retorno não encontrados para plano";
	public static final String ERRO_PLANO_SO_PODE_SER_USADO_SEM_SUBSIDIO = "plano só pode ser usado sem subsídio";
	public static final String  ERRO_PLANO_NAO_ENCONTRADO= "Plano %s não encontrado";
	public static final String KEY_VALIDACAO_BASICA = "validacao-basica";
	public static final String KEY_PRE_VALIDACAO_OFERTA = "pre-validacao-oferta";
	public static final String KEY_VALIDACAO_COMPLETA = "validacao-completa";
	public static final String DEZESSEIS = "16";
	public static final String DOZE = "12";
	public static final String KEY_VALIDACAO_PRE_PROPOSTA = "validacao-pre-proposta";
	public static final String CHAVE_UNICA_PLANOS = "planos:lista-unificada";
	public static final long MENOS_UM = -1;
	public static final long UM = 1;
	public static final String KEY_OV = "OV";
	public static final String KEY_LE = "LE";
	public static final String KEY_AI_E_0KM = "AI_E_0KM";
	public static final String AI_AF_OU_0KM = "AI_AF_OU_0KM";
	public static final String AI_AF_E_0KM = "AI_AF_E_0KM";
	public static final String AI_AF = "AI_AF";
	public static final String N = "N";
	public static final String S = "S";
	public static final String PERIODICIDADE_MENSAL = "M";
	public static final String PERIODICIDADE_BIMESTRAL = "B";
	public static final String PERIODICIDADE_TRIMESTRAL = "T";
	public static final String PERIODICIDADE_ANUAL = "A";
	public static final String PERIODICIDADE_SEMESTRAL = "S";

	public static final String LISTA_DE_PLANOS = "listaDePlanos";
	public static final String PERMITE_BALAO ="P";
	public static final String OBRIGATORIO_BALAO = "O";
	public static final String OBRIGATORIO_BALAO_ULTIMA_PARCELA = "OU";
	public static final String NAO_PERMITE_BALAO = "NP";
	public static final int CINCO = 5;
	public static final long MIL = 1000;
    private Constants() {
    	
    }

}
