package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.utils.LocalDateTimeDeserializer;
import br.com.bancotoyota.services.simulador.utils.LocalDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ComboSeguroGarantia {
    private String descricao;
    private Integer id;
    @JsonProperty("combo-default")
    private Boolean comboDefault;
    @JsonProperty("data-inicio")
    private OffsetDateTime dataInicio;
    @JsonProperty("cnpj-seguradora")
    private String cnpjSeguradora;
    private List<ItemSeguroGarantia> itens;
}
