package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.entities.Seguro;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class OfertasPrestamistaResponse {

    private List<PrazoResidualResponse> simulacoes = new ArrayList<>();

    public OfertasPrestamistaResponse removerReplicas() {
        Set<String> seen = new HashSet<>();

        this.simulacoes = simulacoes.stream()
                .filter(s -> {
                    String key = criarChaveSimulacao(s);

                    if (seen.contains(key)) return false;
                    seen.add(key);
                    return true;
                })
                .collect(Collectors.toList());

        return this;
    }

    private String criarChaveSimulacao(PrazoResidualResponse sim) {
        return String.join("|",
                obterNomeSeguroPrestamista(sim),
                getItemId(sim)
        );
    }

    private static String getItemId(PrazoResidualResponse sim) {
        return Optional.ofNullable(sim)
                .map(PrazoResidualResponse::getDadosSeguroPrestamistaUtilizado)
                .map(Seguro::getItemId)
                .map(Object::toString)
                .orElse("");
    }

    private String obterNomeSeguroPrestamista(PrazoResidualResponse sim) {
        if (sim.getDadosSeguroPrestamistaUtilizado() != null && sim.getDadosSeguroPrestamistaUtilizado().getDescricao() != null) {
            return sim.getDadosSeguroPrestamistaUtilizado().getDescricao();
        }

        return "SPF".equals(sim.getTipoDeSeguroPrestamista()) ? "Seguro Proteção Financeira - SPF" : "Seguro Vida Prestamista - SVP";
    }
}
