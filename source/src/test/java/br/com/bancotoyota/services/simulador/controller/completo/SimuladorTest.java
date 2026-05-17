package br.com.bancotoyota.services.simulador.controller.completo;

import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcDesejadaRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.response.PrazoDesejadaResponse;
import br.com.bancotoyota.services.simulador.beans.response.PrazoResidualResponse;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcDesejadaResponse;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcResidualResponse;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import br.com.bancotoyota.simuladorpropostasservices.entidades.TipoPlanilha;
import br.com.bancotoyota.simuladorpropostasservices.entidades.simulacao.DadosSimulacaoPlanilha;
import br.com.bancotoyota.simuladorpropostasservices.entidades.simulacao.ResultadoCalculado;
import br.com.bancotoyota.simuladorpropostasservices.services.ServicesSimulacao;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;


import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
public class SimuladorTest extends MocksParaTestesCompletos {

    public static final String REQUEST_JSON = "-request.json";

    /**
     * Este método pode ser usado para se criar o JSON baseado numa resposta pra transformar a resposta em código
     * para montar a resposta esperada.
     */
    protected void printList(List<PrazoResidualResponse> list) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        list.forEach(i -> {
            try {
                System.out.println(mapper.writeValueAsString(i));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }

    protected <T> T[] resultadosEsperados(String baseName, String basePath, Class<T> cls) throws IOException {
        String valores = lerArquivo(baseName+"-response.json", basePath);
        return mapper.readValue(valores, getArrayClass(cls));
    }

    protected String lerArquivo(Resource resource) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            return IOUtils.toString(in, "UTF-8");
        }
    }

    protected void executarTeste(Resource resource, PrazoResidualResponse[] resultadosEsperados) throws IOException {

        SimulacaoParcResidualRequest request = mapper.readValue(lerArquivo(resource), SimulacaoParcResidualRequest.class);
        ResponseEntity<SimulacaoParcResidualResponse> response = controller.getParcelaResidual(request);
        SimulacaoParcResidualResponse simulacao = response.getBody();

        List<PrazoResidualResponse> list = simulacao.getPrazos();
        if (resultadosEsperados[0].getValorTotalPrazo() == null) {
            // setando o valor para null para não precisar acertar em todos os arquivos json
            list.forEach(r -> r.setValorTotalPrazo(null));
        }

        assertEquals(HttpStatus.OK,response.getStatusCode());

        // limpando o fluxo para que não tenhamos que colocar essas informações nos JSONs
        list.forEach(r -> {r.setFluxo(null); r.setResultadoCalculado(null); r.setPercentualParcelaResidual(null);});
       
        assertArrayEquals(resultadosEsperados, list.toArray());
    }

    protected void executarTesteDesejada(Resource resource, PrazoDesejadaResponse[] resultadosEsperados) throws IOException {

        SimulacaoParcDesejadaRequest request = mapper.readValue(lerArquivo(resource), SimulacaoParcDesejadaRequest.class);
        ResponseEntity<SimulacaoParcDesejadaResponse> response = ncontroller.getParcelaDesejada(request);
        SimulacaoParcDesejadaResponse simulacao = response.getBody();

        List<PrazoDesejadaResponse> list = simulacao.getPrazos();
        if (resultadosEsperados[0].getValorTotalPrazo() == null) {
            // setando o valor para null para não precisar acertar em todos os arquivos json
            list.forEach(r -> r.setValorTotalPrazo(null));
        }

        assertEquals(HttpStatus.OK,response.getStatusCode());

        list.stream().forEach(p -> p.setFluxo(null)); // pra não precisar comparar o fluxo também
        assertArrayEquals(resultadosEsperados, list.toArray());
    }

    //@Test
    public void executaParcelaResidual() throws IOException {
        System.out.println("número cenários executados para parcela residual: " + executaTestes(RESIDUAL));
    }

    //@Test
    public void executaParcelaDesejada() throws IOException {
        System.out.println("número cenários executados para parcela desejada: " + executaTestes(DESEJADA));
    }

    //@Test
    public void executaUmaSimulacao() throws IOException {
        String str = lerArquivo("dados-simulacao.json","./");
        DadosSimulacaoPlanilha dadosSimulacao = mapper.readValue(str, DadosSimulacaoPlanilha.class);
        System.out.println(dadosSimulacao);
        ResultadoCalculado resultadoCalculado = new ServicesSimulacao().realizaSimulacao(TipoPlanilha.BTB, "1", dadosSimulacao);
        System.out.println(resultadoCalculado.getValorFinanciamento());
        assertEquals(new BigDecimal("75562.35"), NumberUtils.toBigDecimal(resultadoCalculado.getValorFinanciamento()));
    }

    @Test
    public void executaUmaSimulacaoSubsidio() throws IOException {
        String str = lerArquivo("dados-simulacao-subsidio.json","./");
        DadosSimulacaoPlanilha dadosSimulacao = mapper.readValue(str, DadosSimulacaoPlanilha.class);
        System.out.println(dadosSimulacao);
        ResultadoCalculado resultadoCalculado = new ServicesSimulacao().realizaSimulacao(TipoPlanilha.BTB, "1", dadosSimulacao);
        System.out.println(resultadoCalculado.getValorSubsidio());
        System.out.println(dadosSimulacao.getTaxaOperacao());
        assertEquals(new BigDecimal("1000.00"), NumberUtils.toBigDecimal(resultadoCalculado.getValorSubsidio()));
    }

    @Test
    public void executaUmaSimulacaoSubsidioTaxa() throws IOException {
        String str = lerArquivo("dados-simulacao-subsidio-taxa.json","./");
        DadosSimulacaoPlanilha dadosSimulacao = mapper.readValue(str, DadosSimulacaoPlanilha.class);
        System.out.println(dadosSimulacao);
        ResultadoCalculado resultadoCalculado = new ServicesSimulacao().realizaSimulacao(TipoPlanilha.BTB, "1", dadosSimulacao);
        System.out.println(resultadoCalculado.getValorSubsidio());
        System.out.println(dadosSimulacao.getTaxaOperacao());
        assertEquals("0.012199", dadosSimulacao.getTaxaOperacao());
    }

    @Test
    public void executaUmaSimulacaoSubsidioConferencia() throws IOException {
        String str = lerArquivo("dados-simulacao-teste.json","./");
        DadosSimulacaoPlanilha dadosSimulacao = mapper.readValue(str, DadosSimulacaoPlanilha.class);
        System.out.println(dadosSimulacao);
        ResultadoCalculado resultadoCalculado = new ServicesSimulacao().realizaSimulacao(TipoPlanilha.BTB, "1", dadosSimulacao);
        System.out.println(resultadoCalculado.getValorSubsidio());
        System.out.println(resultadoCalculado.getValorParcelaComIOF());
        System.out.println(dadosSimulacao.getTaxaOperacao());
        assertEquals("0.00002629", dadosSimulacao.getTaxaOperacao());
    }

    public int executaTestes(String basePath) throws IOException {

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(basePath + "*" + REQUEST_JSON);

        String testeSelecionado = System.getProperty("testeSelecionado");

        System.out.println("==========================================");
        System.out.println("Diretório: " + basePath);

        int numero = 0;

        for (Resource r : resources) {
            if (testeSelecionado(r, testeSelecionado)) {
                numero ++;
                System.out.println("executando: " + r.getFilename());
                if (r.getFilename().equals("basico-request.json")) {
                    //System.out.println("teste");
                }
                long start = System.currentTimeMillis();

                String name = r.getFilename();
                name = name.substring(0, name.length()-REQUEST_JSON.length());

                if (basePath.startsWith("desejada")) {
                    PrazoDesejadaResponse[] resultadosEsperados = resultadosEsperados(name, basePath, PrazoDesejadaResponse.class);
                    Set<Integer> prazos = Arrays.stream(resultadosEsperados).map(PrazoResidualResponse::getParcelas).collect(Collectors.toSet());
                    prepararMockDeSimulacaoServices(prazos);

                   	executarTesteDesejada(r, resultadosEsperados);
                } else {
                    PrazoResidualResponse[] resultadosEsperados = resultadosEsperados(name, basePath, PrazoResidualResponse.class);
                    Set<Integer> prazos = Arrays.stream(resultadosEsperados).map(PrazoResidualResponse::getParcelas).collect(Collectors.toSet());
                    prepararMockDeSimulacaoServices(prazos);

                   	executarTeste(r, resultadosEsperados);
                }
                long end = System.currentTimeMillis();
                System.out.println("time: " + (end - start));
            }
        }

        return numero;
    }

    private boolean testeSelecionado(Resource r, String testeSelecionado) {
        if (testeSelecionado == null) {
            return true;
        }
        String name = r.getFilename();
        int i = name.lastIndexOf(REQUEST_JSON);
        if (i >= 0) {
            name = name.substring(0, i);
            return name.equals(testeSelecionado);
        } else {
            return false;
        }
    }
}
