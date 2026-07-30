package de.training.taskapi.domain;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Fachliches, persistenzunabhängiges Task-Modell.
 */
public record Task(
        Long id,
        String title,
        String description,
        TaskPriority priority,
        LocalDate dueDate,
        boolean completed,
        Instant createdAt,
        Instant updatedAt
) {
}
