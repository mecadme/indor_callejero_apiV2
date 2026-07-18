package com.indorcallejero.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

// VIA_DTO: sin esto, Spring Data serializa Page<T> directo (PageImpl), algo
// que el propio framework marca como "no soportado" para JSON estable --
// la forma exacta puede cambiar entre versiones de Spring Data sin previo
// aviso. Lo vimos como WARN en el log recién, probando el listado paginado
// de jugadores de la Etapa 6; se aplica a todo endpoint paginado del
// proyecto (Usuario, Equipo, Jugador), no solo a este.
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@SpringBootApplication
public class IndorCallejeroApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(IndorCallejeroApiApplication.class, args);
	}

}
