package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.repository.RedisRepository;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.awaitility.Awaitility;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = {
        MongoAutoConfiguration.class,
        MongoRepositoriesAutoConfiguration.class,
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class,
    }
)
@ActiveProfiles("test")
public class SimuladorExceptionControllerWebTest {

    @LocalServerPort
    private int port;
    @MockBean
    private SeguroPrestamistaPlusServiceImpl prestamistaService;
    @MockBean
    private DataCarencia dataCarencia;

    @MockBean
    private PlanoService planoService;

    @MockBean
    private MotorTaxaPlanoService motorTaxaPlanoService;

    @MockBean
    private RedisRepository redisRepository;

    @MockBean
    private ClientService clientService;
    @MockBean
    private CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices;
    @Autowired
    private SimuladorParcResidualController controller;

    @Test
    public void testeDeThrowable() throws Exception {
        when(dataCarencia.getDataCalculo()).thenThrow(new IllegalStateException());
        HttpClientBuilder builder = HttpClients.custom();
        HttpClient httpClient = builder.build();
        HttpGet getMethod = new HttpGet(
                "http://localhost:" + port + "/financiamentos/data-calculo");
        HttpResponse response = httpClient.execute(getMethod);
        assertEquals(500, response.getStatusLine().getStatusCode());
        String json = IOUtils.toString(response.getEntity().getContent());

        // Utilizado o contains em vez do equals para o teste unitário não quebrar na comparação da linha do erro na exception
        assertTrue(json.contains("{\"erros\":[\"Erro interno - Causa java.lang.IllegalStateException: Optional[br.com.bancotoyota.services.simulador.controller.SimuladorParcResidualController.getDataBA(SimuladorParcResidualController.java"));
    }

    private void tryLock(Lock lock) {
        try {
            if (!lock.tryLock(60, TimeUnit.SECONDS)) {
                fail("erro esperando o lock");
            }
        } catch (InterruptedException e) {
            fail("tryLock interrompido");
        }
    }

    private void await(Condition condition) {
        try {
            if (!condition.await(60, TimeUnit.SECONDS)) {
                fail("erro esperando o lock");
            }
        } catch (InterruptedException e) {
            fail("tryLock interrompido");
        }
    }

    @Ignore
    @Test
    public void testeComHttpClient() throws Exception {
        controller.setAborted(false);
        Lock lockAntesDoAbort = new ReentrantLock();
        Condition conditionAntesDoAbort = lockAntesDoAbort.newCondition();

        Lock lockDaResposta = new ReentrantLock();

        Lock lockInicio = new ReentrantLock();
        Condition conditionInicio = lockInicio.newCondition();

        when(dataCarencia.getDataCalculo()).thenAnswer(i -> {
            System.out.println("request - request em execução");
            tryLock(lockAntesDoAbort);
            try {
                System.out.println("request - obtido o lockAntesDoAbort, fazendo signal da conditionAntesDoAbort");
                conditionAntesDoAbort.signal();
            } finally {
                lockAntesDoAbort.unlock();
            }
            System.out.println("request - lockAntesDoAbort liberado, pegando lockDaResposta");
            tryLock(lockDaResposta);
            try {
                System.out.println("request - retornando resposta");
                return LocalDate.now();
            } finally {
                lockDaResposta.unlock();
            }
        });
        HttpClientBuilder builder = HttpClients.custom();
        HttpClient httpClient = builder.build();
        HttpGet getMethod = new HttpGet(
                "http://localhost:" + port + "/financiamentos/data-calculo");
        Thread t = new Thread(() -> {
            tryLock(lockDaResposta);
            try {
                System.out.println("thread - lockDaResposta obtido");

                System.out.println("thread - pegando lockInicio");
                tryLock(lockInicio);
                try {
                    System.out.println("thread - sinalizando conditionInicio");
                    conditionInicio.signal();
                } finally {
                    lockInicio.unlock();
                }

                System.out.println("thread - esperando request estar em execução");
                long start = System.currentTimeMillis();
                tryLock(lockAntesDoAbort);
                try {
                    await(conditionAntesDoAbort);
                } finally {
                    lockAntesDoAbort.unlock();
                }
                long end = System.currentTimeMillis();
                System.out.println("thread - tempo de espera: " + (end - start));
                System.out.println("thread - abortando o request HTTP antes que a resposta seja dada");
                getMethod.abort();
                assertTrue(getMethod.isAborted());
            } finally {
                System.out.println("thread - liberando resposta do request");
                lockDaResposta.unlock();
            }
        });
        t.setDaemon(true);
        t.start();

        // A thread tem que pegar o lockDaResposta em primeiro lugar
        boolean locked = lockDaResposta.tryLock();
        if (locked) {
            // Se pegamos o lock aqui então vamos esperar a thread pegar para continuar.
            // Primeiro liberamos o lock que a thread precisa pegar.
            lockDaResposta.unlock();
            // E agora esperamos a thread pegar o lock.
            tryLock(lockInicio);
            try {
                System.out.println("main - esperando conditionInicio");
                await(conditionInicio);
                System.out.println("main - conditionInicio sinalizado");
            } finally {
                lockInicio.unlock();
            }
        }

        try {
            System.out.println("main - enviando request");
            httpClient.execute(getMethod);
        } catch (Exception ex) {
            System.out.println("main - erro no request: " + ex.getMessage());
            // esperamos um erro aqui e isso não é um problema
        }

        long start = System.currentTimeMillis();
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> controller.isAborted());
        long end = System.currentTimeMillis();
        System.out.println("tempo de espera para gerar o log: " + (end - start));
        assertTrue(controller.isAborted());
    }
}
