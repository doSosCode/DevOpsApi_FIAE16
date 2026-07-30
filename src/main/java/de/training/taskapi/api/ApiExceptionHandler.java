package de.training.taskapi.api;

import de.training.taskapi.domain.TaskNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Übersetzt interne Exceptions in stabile, absichtlich knappe API-Fehlerantworten.
 * Technische Details werden nur serverseitig protokolliert.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final URI TASK_NOT_FOUND = URI.create("urn:task-api:problem:task-not-found");
    private static final URI VALIDATION_FAILED = URI.create("urn:task-api:problem:validation-failed");
    private static final URI CONFLICT = URI.create("urn:task-api:problem:concurrent-update");
    private static final URI INTERNAL_ERROR = URI.create("urn:task-api:problem:internal-error");

    @ExceptionHandler(TaskNotFoundException.class)
    ProblemDetail handleNotFound(TaskNotFoundException exception, HttpServletRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Ressource nicht gefunden");
        problem.setType(TASK_NOT_FOUND);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Die Anfrage enthält ungültige Werte."
        );
        problem.setTitle("Validierungsfehler");
        problem.setType(VALIDATION_FAILED);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("fields", fieldErrors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Ein Pfad- oder Abfrageparameter ist ungültig."
        );
        problem.setTitle("Ungültiger Parameter");
        problem.setType(VALIDATION_FAILED);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentUpdate(HttpServletRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Die Task wurde zwischenzeitlich verändert. Bitte Daten neu laden und erneut versuchen."
        );
        problem.setTitle("Änderungskonflikt");
        problem.setType(CONFLICT);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unerwarteter Fehler bei {} {}", request.getMethod(), request.getRequestURI(), exception);

        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Die Anfrage konnte wegen eines internen Fehlers nicht verarbeitet werden."
        );
        problem.setTitle("Interner Serverfehler");
        problem.setType(INTERNAL_ERROR);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
