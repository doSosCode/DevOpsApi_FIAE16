package de.training.taskapi.service;

import de.training.taskapi.domain.Task;
import de.training.taskapi.domain.TaskPriority;
import de.training.taskapi.domain.TaskRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestriert die Anwendungsfälle der Task-API.
 *
 * <p>Validierung der HTTP-Nutzdaten erfolgt am API-Rand. Diese Klasse übernimmt
 * Normalisierung, Transaktionsgrenzen und fachliche Ablaufsteuerung.</p>
 */
@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final Clock clock;

    public TaskService(TaskRepository taskRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    public List<Task> findAll(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), TaskRepository.MAX_LIST_SIZE);
        return taskRepository.findFirst(safeLimit);
    }

    public Task findById(long id) {
        return taskRepository.findById(id);
    }

    @Transactional
    public Task create(String title, String description, TaskPriority priority, LocalDate dueDate) {
        return taskRepository.create(
                normalizeRequiredText(title),
                normalizeDescription(description),
                priority,
                dueDate,
                clock.instant()
        );
    }

    @Transactional
    public Task update(long id, String title, String description, TaskPriority priority, LocalDate dueDate) {
        return taskRepository.update(
                id,
                normalizeRequiredText(title),
                normalizeDescription(description),
                priority,
                dueDate,
                clock.instant()
        );
    }

    @Transactional
    public Task setCompleted(long id, boolean completed) {
        return taskRepository.setCompleted(id, completed, clock.instant());
    }

    @Transactional
    public void delete(long id) {
        taskRepository.delete(id);
    }

    private static String normalizeRequiredText(String value) {
        return value.trim();
    }

    private static String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }
}
