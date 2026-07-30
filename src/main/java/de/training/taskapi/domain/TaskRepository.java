package de.training.taskapi.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Fachlicher Port für die Task-Persistenz.
 *
 * <p>Die Anwendungsschicht kennt dadurch weder JPA noch Spring Data. Der technische
 * Datenbankadapter implementiert dieses Interface.</p>
 */
public interface TaskRepository {

    int MAX_LIST_SIZE = 100;

    List<Task> findFirst(int limit);

    Task findById(long id);

    Task create(
            String title,
            String description,
            TaskPriority priority,
            LocalDate dueDate,
            Instant now
    );

    Task update(
            long id,
            String title,
            String description,
            TaskPriority priority,
            LocalDate dueDate,
            Instant now
    );

    Task setCompleted(long id, boolean completed, Instant now);

    void delete(long id);
}
