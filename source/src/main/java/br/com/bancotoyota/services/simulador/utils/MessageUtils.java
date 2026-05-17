package br.com.bancotoyota.services.simulador.utils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Luis Santos
 * Utilitário das mensagens internacionalizadas do sistema.
 */
public class MessageUtils {
	private String nomeBaseResourceBundle;
    private ResourceBundle resourceBundle;
    private Locale localeBundle;

    /**
     * Construtor com uma classe que será utilizada para resgatar os valores internacionalizados.<br/>
     * A localização do arquivo é definida pelo pacote da classe informada concatenada com o nome padrao "i18n".<br/>
     * Será utilizado o {@link Locale} {@link Locale#getDefault() padrão} da aplicação.
     *
     * @param clazz classe utilizada para construir o nome base (base-name) dos valores internacionalizados.
     */
    public MessageUtils(Class<?> clazz) {
        this(clazz, Locale.getDefault());
    }

    /**
     * Construtor com uma classe que será utilizada para resgatar os valores internacionalizados.<br/>
     * A localização do arquivo é definida pelo pacote da classe informada concatenada com o nome padrão "i18n".<br/>
     *
     * @param clazz classe utilizada para construir o nome base (base-name) dos valores internacionalizados.
     * @param locale o {@link Locale} que sera utilizado para resgatar os valores internacionalizados.
     */
    public MessageUtils(Class<?> clazz, Locale locale) {
        this(clazz.getPackage().getName().concat(".i18n"), locale);
    }

    /**
     * Construtor com o nome base (base-name) definido para os valores internacionalizados.<br/>
     * Será utilizado o {@link Locale} {@link Locale#getDefault() padrão} da aplicação.
     *
     * @param nomeBaseResourceBundle o nome base (base-name) dos valores internacionalizados.
     */
    public MessageUtils(String nomeBaseResourceBundle) {
        this(nomeBaseResourceBundle, Locale.getDefault());
    }

    /**
     * Construtor com o nome base (base-name) definido para os valores internacionalizados.<br/>
     *
     * @param nomeBaseResourceBundle o nome base (base-name) dos valores internacionalizados.
     * @param locale o {@link Locale} que será utilizado para resgatar os valores internacionalizados.
     */
    public MessageUtils(String nomeBaseResourceBundle, Locale locale) {
        this.localeBundle = locale;
        this.nomeBaseResourceBundle = nomeBaseResourceBundle;
    }

    /**
     * Recupera a mensagem a partir do resource-bundle informado.
     *
     * @param key a chave da mensagem no resource-bundle.
     * @return a mensagem para a chave informada.
     */
    public String getMessage(String key) {
        return getMessage(key, null);
    }

    /**
     * Recupera a mensagem a partir do resource-bundle informado.
     *
     * @param key a chave da mensagem no resource-bundle.
     * @param args argumentos para substituir na mensagem.
     * @return a mensagem para a chave informada.
     */
    public String getMessage(String key, Object[] args) {
        if (resourceBundle == null) {
            resourceBundle = ResourceBundle.getBundle(nomeBaseResourceBundle,
                    localeBundle, Thread.currentThread().getContextClassLoader());
        }
        String message = resourceBundle.getString(key);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                Object obj = args[i];
                if (obj instanceof String) {
                    String text = resourceBundle.getString((String) obj);
                    args[i] = text;
                }

            }
            message = MessageFormat.format(message, args);
        }
        return message;
    }

    /**
     * Recupera a mensagem a partir do resource-bundle informado.
     *
     * @param enumValue o {@link Enum} para compor a chave da mensagem no resource-bundle.
     * @return a mensagem para a chave informada.
     */
    public String getMessage(Enum<?> enumValue) {
        String key = enumValue.getClass().getSimpleName().concat("." + enumValue.name());
        return this.getMessage(key, null);
    }
}
