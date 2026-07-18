package com.indorcallejero.api.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;

/**
 * PERF-04 del audit: 0 usos de @Cacheable en el proyecto original, así que
 * cada carga de la tabla de posiciones o el calendario pega contra MySQL,
 * aunque nada haya cambiado desde la última consulta hace un segundo.
 *
 * La invalidación real es por evento (@CacheEvict en StandingService y
 * MatchService cuando algo escribe), no por el TTL de acá abajo -- ese
 * TTL es red de seguridad, ver el comentario en application.yml.
 *
 * JSON, no el serializador JDK que Spring usa por default para el valor
 * cacheado: StandingDTO/MatchDTO son records, y un record no implementa
 * Serializable solo por ser record -- con el serializador default,
 * @Cacheable hubiera explotado con NotSerializableException recién en
 * runtime, al primer intento de guardar algo en Redis.
 *
 * GenericJacksonJsonRedisSerializer (Jackson 3, tools.jackson.*), no su
 * equivalente de Jackson 2 (com.fasterxml.jackson.*, deprecado desde
 * Spring Data Redis 4.0 y ya marcado para remoción): probado en vivo, el
 * serializador de Jackson 2 no puede reconstruir un Page<T> cacheado
 * (PageImpl no tiene constructor default ni Creator que Jackson 2
 * reconozca -- InvalidDefinitionException recién al segundo request,
 * cuando intenta leer lo que el primer request escribió). El mismo
 * proyecto ya usa Jackson 3 en el resto de la app (ver el DTO mapping),
 * así que esto además evita mezclar dos versiones de Jackson.
 *
 * enableDefaultTyping(validator), probado en vivo también: sin esto, el
 * JSON cacheado no lleva metadata de tipo, y al leerlo Jackson devuelve un
 * LinkedHashMap genérico en vez de reconstruir el Page real -- el cache
 * casteaba mal y tiraba ClassCastException en el segundo request. El
 * validator no es "unsafe": solo permite reconstruir tipos de este
 * paquete, colecciones de java.util y las clases de paginado de Spring
 * Data, así que un valor cacheado no puede forzar a Jackson a instanciar
 * una clase arbitraria (el gadget-attack clásico de default typing sin
 * acotar).
 *
 * allowIfSubType, no (solo) allowIfBaseType: probado en vivo, cachear un
 * Page con contenido (no una página vacía) tira InvalidTypeIdException al
 * leer -- Jackson resuelve el tipo de cada elemento de una lista contra
 * Object (el tipo declarado del propio serializador genérico), no contra
 * el paquete del elemento real, así que allowIfBaseType("com.indorcallejero
 * .api.") nunca matchea ahí. allowIfSubType mira la clase concreta que se
 * va a instanciar (MatchDTO, StandingDTO, ...) sin importar contra qué
 * tipo declarado se está resolviendo.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.indorcallejero.api.")
                .allowIfSubType("org.springframework.data.domain.")
                .allowIfSubType("java.util.")
                .build();

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(GenericJacksonJsonRedisSerializer.builder()
                                .enableDefaultTyping(typeValidator)
                                .build()));
    }
}
