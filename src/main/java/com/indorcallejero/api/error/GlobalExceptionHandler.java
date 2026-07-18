package com.indorcallejero.api.error;

import com.indorcallejero.api.auth.TooManyAttemptsException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * ARQ-01/ARQ-02 del audit: acá es donde toda excepción se convierte en un
 * código HTTP DECIDIDO, no en lo que Spring haga por default. Ojo con lo
 * que esta clase NO cubre: las denegaciones que Spring Security resuelve
 * ANTES de llegar al controller (una request sin token contra una ruta
 * "authenticated()", o el denyAll() de última línea) nunca disparan un
 * @ExceptionHandler -- esas se manejan aparte, en
 * RestAuthenticationEntryPoint / RestAccessDeniedHandler, que SecurityConfig
 * conecta directo al filtro. Dos mecanismos distintos porque son dos
 * momentos distintos del ciclo de vida del request.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyAttempts(HttpServletRequest request) {
        return build(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Esperá antes de volver a intentar.", request);
    }

    // Cubre BadCredentialsException (login con clave incorrecta) y
    // cualquier otra AuthenticationException que se origine DENTRO de un
    // método de controller -- distinto del caso "no mandaste token", que
    // ni siquiera llega hasta acá.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", request);
    }

    // Cubre @PreAuthorize (ej. el self-or-admin de UserController) --
    // AccessDeniedException del denyAll() de SecurityConfig se maneja en
    // RestAccessDeniedHandler, no acá.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "No tenés permiso para esta acción", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "El recurso ya existe o viola una restricción de datos", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.validation(request.getRequestURI(), details));
    }

    // Errores de binding de Spring MVC que no son de negocio (JSON mal
    // formado, falta un header/param requerido, tipo equivocado en un
    // path variable). Sin este handler, el Exception.class de más abajo
    // los convertiría en 500 -- son 400, siempre fueron un problema del
    // request, no del servidor.
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Solicitud inválida o mal formada", request);
    }

    // Red de última línea: cualquier excepción no prevista termina acá, no
    // en la página de error por defecto de Spring Boot con el stack trace
    // adentro. El cliente recibe un mensaje genérico; el stack trace real
    // va al log del servidor, donde corresponde.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Excepción no controlada en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message, request.getRequestURI()));
    }
}
