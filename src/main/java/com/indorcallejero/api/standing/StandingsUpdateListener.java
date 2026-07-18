package com.indorcallejero.api.standing;

import com.indorcallejero.api.match.MatchResultRecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * PERF-03 del audit, la pieza que faltaba: UpdateStandingsEventListener
 * original era un ApplicationListener simple, síncrono, en el mismo hilo
 * y la misma transacción que registrar el resultado. Acá:
 *
 * - @TransactionalEventListener(AFTER_COMMIT): el listener ni arranca si
 *   la transacción de MatchService.recordResult() no comete -- no tiene
 *   sentido recalcular standings de un resultado que se revirtió.
 * - @Async("standingsExecutor"): corre en el pool dedicado de AsyncConfig,
 *   no en el hilo de Tomcat que atendió el request ni en el default
 *   SimpleAsyncTaskExecutor de Spring (que crea un hilo nuevo sin límite
 *   por cada llamada -- PERF-07 del audit).
 */
@Component
public class StandingsUpdateListener {

    private static final Logger log = LoggerFactory.getLogger(StandingsUpdateListener.class);
    private static final int MAX_RETRIES = 3;

    private final StandingService standingService;

    public StandingsUpdateListener(StandingService standingService) {
        this.standingService = standingService;
    }

    @Async("standingsExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchResultRecorded(MatchResultRecordedEvent event) {
        applyWithRetry(event.homeTeamId(), event.goalsHomeTeam(), event.goalsAwayTeam());
        applyWithRetry(event.awayTeamId(), event.goalsAwayTeam(), event.goalsHomeTeam());
    }

    private void applyWithRetry(Long teamId, int goalsFor, int goalsAgainst) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                standingService.recordMatchResult(teamId, goalsFor, goalsAgainst);
                return;
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.warn("Conflicto de versión actualizando standing del equipo {} (intento {}/{})",
                        teamId, attempt, MAX_RETRIES);
                if (attempt == MAX_RETRIES) {
                    log.error("Se agotaron los reintentos actualizando standing del equipo {} -- "
                            + "standing desactualizado hasta la próxima corrección manual", teamId, ex);
                }
            }
        }
    }
}
