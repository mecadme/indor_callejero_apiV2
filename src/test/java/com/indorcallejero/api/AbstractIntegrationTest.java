package com.indorcallejero.api;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base para tests de integración: levanta un MySQL real en Docker, no H2.
 * ARQ-07 del audit pide justo esto -- H2 no habla el mismo dialecto SQL que
 * producción (ni el mismo comportamiento de constraints, ni el mismo motor
 * de fechas/enums), así que un test verde contra H2 no prueba lo que
 * importa probar.
 *
 * El contenedor es "static": todas las clases que extienden esta base
 * comparten UNA sola instancia de MySQL por corrida de test suite -- se
 * levanta una vez, no una vez por clase (arrancar un contenedor Docker
 * nuevo por clase de test sería correcto pero lento).
 */
@Testcontainers
@SpringBootTest
@ExtendWith(SpringExtension.class)
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");
}
