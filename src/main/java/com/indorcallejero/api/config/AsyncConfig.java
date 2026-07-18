package com.indorcallejero.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * PERF-07 del audit: sin este bean, @Async usa el SimpleAsyncTaskExecutor
 * por defecto de Spring -- que no es un pool, crea un hilo NUEVO por cada
 * llamada, sin límite. Bajo carga (muchos resultados cargándose a la vez),
 * eso es tan peligroso como no tener límite en el pool de Hikari (PERF-01,
 * Etapa 6): nada frena la creación de hilos hasta que el proceso se queda
 * sin memoria.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfig {

    @Bean("standingsExecutor")
    public Executor standingsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("standings-");
        executor.initialize();
        return executor;
    }
}
