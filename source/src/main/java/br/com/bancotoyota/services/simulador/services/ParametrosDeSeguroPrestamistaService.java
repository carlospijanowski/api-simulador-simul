package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.entities.DadosCliente;
import br.com.bancotoyota.services.simulador.entities.EnumErroValidacao;
import br.com.bancotoyota.services.simulador.entities.OrigemNegocio;
import br.com.bancotoyota.services.simulador.entities.ValorComprometidoSeguroPrestamista;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class ParametrosDeSeguroPrestamistaService {

    private ClientService clientService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(Constants.FORMATO_DATA_BRASIL);

    @Autowired
    public ParametrosDeSeguroPrestamistaService(ClientService clientService) {
        this.clientService = clientService;
    }

    public LocalDate getDataNascimento(LocalDate dataNascimento, String cpfProponente, String token, List<Erro> list) {
        if (dataNascimento == null) {
            try {
                DadosCliente dadosCliente = clientService.getDadosCliente(cpfProponente);
                if (dadosCliente != null) {
                    dataNascimento = LocalDate.parse(dadosCliente.getDataNascimento(), FORMATTER);
                }
            } catch (ThirdPartyException ex) {
                log.error("erro ao obter data de nascimento", ex);
                if (list != null) {
                    list.add(new Erro(null, null, EnumErroValidacao.ERRO_API_CLIENTES_INDISPONIVEL.getKey(),
                            EnumErroValidacao.ERRO_API_CLIENTES_INDISPONIVEL.getValue()));
                }
            }
        }
        return dataNascimento;
    }

    public ParametrosDeSeguroPrestamista getParametros(OrigemNegocio origemNegocio, String cpfProponente, String token,
                                                       ParametrosDeSeguroPrestamista original, List<Erro> list) {
        ValorComprometidoSeguroPrestamista.ClienteComprometimentoSeguro cs;
        if (cpfProponente != null) {
            // o serviço não retorna 404 mesmo quando não encontra o CPF e retorna zero nos valores, que é o que precisamos
            try {
                cs = clientService.getValoresComprometidos(cpfProponente);
            } catch (ThirdPartyException ex) {
                log.error("erro ao obter valores comprometidos", ex);
                if (list != null) {
                    list.add(new Erro(null, null, EnumErroValidacao.ERRO_API_CLIENTES_INDISPONIVEL.getKey(),
                            EnumErroValidacao.ERRO_API_CLIENTES_INDISPONIVEL.getValue()));
                }
                cs = semValorComprometido();
            }
        } else {
            cs = semValorComprometido();
        }

        ParametrosDeSeguroPrestamista params = new ParametrosDeSeguroPrestamista(false, false,
                origemNegocio.getSpfDisponivel(),
                origemNegocio.getSvpDisponivel(),
                origemNegocio.getSvpSeSpfRemovido(), cs.getSpfValorComprometido(),
                cs.getSvpValorComprometido(), null, null,true,true,origemNegocio.getCnpj(),null,null);
        if (original != null) {
            params.setValorOriginalTotalFinanciado(original.getValorOriginalTotalFinanciado());
            params.setSeguroPrestamistaEscolhido(original.getSeguroPrestamistaEscolhido());
            params.setPermiteSPFComBaseNumeroContratos(original.getPermiteSPFComBaseNumeroContratos());
            params.setPermiteSVPComBaseNumeroContratos(original.getPermiteSVPComBaseNumeroContratos());
            params.setSegurosPrestamistasExcluidos(original.getSegurosPrestamistasExcluidos());
            params.setIdSeguroEscolhido(original.getIdSeguroEscolhido());
        }
        return params;
    }

    private ValorComprometidoSeguroPrestamista.ClienteComprometimentoSeguro semValorComprometido() {
        ValorComprometidoSeguroPrestamista.ClienteComprometimentoSeguro cs =
            new ValorComprometidoSeguroPrestamista.ClienteComprometimentoSeguro();
        cs.setSpfValorComprometido(BigDecimal.ZERO);
        cs.setSvpValorComprometido(BigDecimal.ZERO);
        return cs;
    }
}
