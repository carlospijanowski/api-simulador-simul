package br.com.bancotoyota.services.simulador.repository;

import br.com.bancotoyota.services.simulador.entities.ControlesBA;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.entities.TaxasIOF;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * @author Luis Santos Repositório de consulta no Redis
 */

@Repository
@Profile({ "default", "dev", "hml", "prod", "gft" })
public class RedisRepositoryImpl implements RedisRepository {

	public static final String SUBSIDIO_VARIAVEL = "subsídio variável (";
	private ListOperations<String, TaxasIOF> listOperations;
	private SetOperations<String, Seguro> seguroSetOperations;
	private ListOperations<String, ControlesBA> controleBAOperation;

	@Autowired
	public RedisRepositoryImpl(RedisTemplate<String, TaxasIOF> redisIOFTemplate,
			RedisTemplate<String, Seguro> seguroRedisTemplate,
			RedisTemplate<String, ControlesBA> controlesBARedisTemplate) {
		this.listOperations = redisIOFTemplate.opsForList();
		this.seguroSetOperations = seguroRedisTemplate.opsForSet();
		this.controleBAOperation = controlesBARedisTemplate.opsForList();
	}

	public TaxasIOF findTaxasIOF(String key) {

		TaxasIOF taxasIOF = listOperations.index(key, 0);
		if (taxasIOF == null) {
			throw new EntityNotFoundException(TaxasIOF.class, key);
		}
		return taxasIOF;
	}

	public Seguro getSeguro(String codigo) {
		Set<Seguro> set = seguroSetOperations.members("seguros:" + codigo);

		return set.stream().findFirst().orElseThrow(() -> new EntityNotFoundException(Seguro.class, codigo));
	}

	public ControlesBA getDataBA() {
		ControlesBA controlesBA = controleBAOperation.index("controles-ba", 0);
		if (controlesBA == null) {
			throw new EntityNotFoundException(ControlesBA.class, "0");
		}
		return controlesBA;
	}
}
