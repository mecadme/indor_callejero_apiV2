package com.indorcallejero.api;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base para tests de integración: levanta un MySQL real en Docker, no H2.
 * ARQ-07 del audit pide justo esto -- H2 no habla el mismo dialecto SQL que
 * producción (ni el mismo comportamiento de constraints, ni el mismo motor
 * de fechas/enums), así que un test verde contra H2 no prueba lo que
 * importa probar.
 *
 * Arranque manual en un bloque static {}, no @Testcontainers/@Container +
 * @ServiceConnection. Con las anotaciones, AuthFlowIntegrationTest y
 * TeamPlayerIntegrationTest terminaban levantando DOS contenedores en vez
 * de reusar uno -- el segundo container arrancaba con el pool de Hikari
 * del primero todavía apuntándole a una conexión ya cerrada, y las cuatro
 * pruebas de la segunda clase morían en el timeout de 30s del pool.
 * @DynamicPropertySource es el patrón más viejo y más simple: un solo
 * bloque static por JVM, sin que el descubrimiento de @ServiceConnection
 * (todavía nuevo en Boot 4.1) decida si dos clases de test "son la misma
 * configuración" o no.
 *
 * @ActiveProfiles("test") activa application-test.yml como CAPA sobre el
 * application.yml real -- no lo reemplaza (a diferencia de un
 * application.yml homónimo en test/resources, el error que se encontró
 * armando la Etapa 9: management.endpoints.web.exposure.include del
 * archivo real no estaba llegando a ningún test).
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public abstract class AbstractIntegrationTest {

    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    // Redis real en Docker acá también, no un fake en memoria -- un fake no
    // reproduce el TTL nativo (RedisTokenBlacklist) ni la atomicidad de
    // INCR (RedisRateLimiter), que son justo lo que estas clases explotan.
    static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:8-alpine"));

    static {
        mysql.start();
        redis.start();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
}
