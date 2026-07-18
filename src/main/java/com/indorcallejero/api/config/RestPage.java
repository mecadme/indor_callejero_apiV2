package com.indorcallejero.api.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * PageImpl sabe serializarse a JSON (vía sus getters) pero no
 * deserializarse: no tiene constructor default ni ningún @JsonCreator, así
 * que Jackson no puede reconstruirla. Probado en vivo cacheando
 * MatchService.getMatches en Redis: el primer request escribía bien, el
 * segundo (cache hit, leyendo lo ya escrito) explotaba con
 * InvalidDefinitionException.
 *
 * RestPage repite los cuatro campos que el serializador plano de PageImpl
 * escribe (content, number, size, totalElements) en un constructor propio
 * que Jackson sí puede invocar. Los demás campos que PageImpl serializa
 * (pageable, sort, first, last, totalPages, empty, numberOfElements) son
 * derivables de esos cuatro, así que se ignoran al leer.
 *
 * Los servicios que cachean un Page (MatchService, StandingService) tienen
 * que devolver explícitamente un RestPage, no cualquier Page: el tipo
 * concreto en tiempo de ejecución es lo que Jackson graba en "@class" para
 * saber qué reconstruir en el cache hit, y un PageImpl plano (lo que
 * devuelve matchRepository.findAll(...).map(...)) no alcanza.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestPage<T> extends PageImpl<T> {

    @JsonCreator
    public RestPage(
            @JsonProperty("content") List<T> content,
            @JsonProperty("number") int number,
            @JsonProperty("size") int size,
            @JsonProperty("totalElements") long totalElements) {
        super(content, PageRequest.of(number, Math.max(size, 1)), totalElements);
    }

    public RestPage(Page<T> page) {
        super(page.getContent(), page.getPageable(), page.getTotalElements());
    }
}
