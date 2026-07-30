package de.training.taskapi.api;

import de.training.taskapi.domain.Task;
import de.training.taskapi.domain.TaskPriority;
import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        long id,
        String title,
        String description,
        TaskPriority priority,
        LocalDate dueDate,
        boolean completed,
        Instant createdAt,
        Instant updatedAt
) {
    static TaskResponse from(Task task) {
        return new TaskResponse(
                task.id(), task.title(), task.description(), task.priority(), task.dueDate(),
                task.completed(), task.createdAt(), task.updatedAt()
        );
    }
}
