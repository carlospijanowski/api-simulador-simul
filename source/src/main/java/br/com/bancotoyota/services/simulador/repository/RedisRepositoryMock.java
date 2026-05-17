package br.com.bancotoyota.services.simulador.repository;

import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.entities.ControlesBA;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.entities.TaxasIOF;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

/**
 * Usado na geração do CDS apenas durante a build.
 */
@Repository
@Profile({"cds","swagger"})
public class RedisRepositoryMock implements RedisRepository {
    @Override
    public TaxasIOF findTaxasIOF(String key) {
        return getTaxasIOF();
    }

    @Override
    public Seguro getSeguro(String codigo) {
        switch (codigo) {
            case "SVP":
                return getSeguroSVP();
            case "SPF":
                return getSeguroSPF();
            default:
                throw new EntityNotFoundException(Seguro.class, codigo);
        }
    }

    private TaxasIOF getTaxasIOF() {
        return new TaxasIOF("365.000000", "0.030000", "0.003800");
    }

    private Seguro getSeguroSPF() {
        return new Seguro(TipoDeSeguroPrestamista.SPF, new BigDecimal(2500), new BigDecimal("150000.00"),
                new BigDecimal("450000.00"), new BigDecimal("0.029000"), 18, 65);
    }

    private Seguro getSeguroSVP() {
        return new Seguro(TipoDeSeguroPrestamista.SVP, BigDecimal.ZERO, new BigDecimal("330000.00"),
                new BigDecimal("990000.00"), new BigDecimal("0.012000"), 18, 65);
    }

	@Override
	public ControlesBA getDataBA() {
		return new ControlesBA();
	}
}
