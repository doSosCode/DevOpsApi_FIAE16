package de.training.taskapi.database;

import de.training.taskapi.domain.TaskPriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tasks")
class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected TaskEntity() {
        // Von JPA benötigt.
    }

    TaskEntity(String title, String description, TaskPriority priority, LocalDate dueDate, Instant now) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.completed = false;
        this.createdAt = now;
        this.updatedAt = now;
    }

    Long id() { return id; }
    String title() { return title; }
    String description() { return description; }
    TaskPriority priority() { return priority; }
    LocalDate dueDate() { return dueDate; }
    boolean completed() { return completed; }
    Instant createdAt() { return createdAt; }
    Instant updatedAt() { return updatedAt; }

    void update(String title, String description, TaskPriority priority, LocalDate dueDate, Instant now) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.updatedAt = now;
    }

    void markCompleted(boolean completed, Instant now) {
        this.completed = completed;
        this.updatedAt = now;
    }
}
