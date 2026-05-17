package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Document(collection = "data-ba")
@ToString
public class DataBA {

    @Id
    private String id;

    @JsonProperty("data-atual-ba")
    private LocalDate atual;

    public DataBA(LocalDate atual) {
        this.atual = atual;
    }
}
