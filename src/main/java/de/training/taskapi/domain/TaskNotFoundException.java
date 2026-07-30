package de.training.taskapi.domain;

public final class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(long id) {
        super("Task mit ID %d wurde nicht gefunden.".formatted(id));
    }
}
