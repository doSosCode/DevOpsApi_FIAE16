package de.training.taskapi.database;

import de.training.taskapi.domain.Task;
import de.training.taskapi.domain.TaskNotFoundException;
import de.training.taskapi.domain.TaskPriority;
import de.training.taskapi.domain.TaskRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/** JPA-Adapter für den fachlichen Persistenz-Port. */
@Repository
public class JpaTaskStore implements TaskRepository {

    private final SpringDataTaskRepository repository;

    public JpaTaskStore(SpringDataTaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Task> findFirst(int limit) {
        var page = PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "id"));
        return repository.findAll(page).stream().map(JpaTaskStore::toDomain).toList();
    }

    @Override
    public Task findById(long id) {
        return toDomain(findEntity(id));
    }

    @Override
    public Task create(
            String title,
            String description,
            TaskPriority priority,
            LocalDate dueDate,
            Instant now
    ) {
        return toDomain(repository.save(new TaskEntity(title, description, priority, dueDate, now)));
    }

    @Override
    public Task update(
            long id,
            String title,
            String description,
            TaskPriority priority,
            LocalDate dueDate,
            Instant now
    ) {
        var entity = findEntity(id);
        entity.update(title, description, priority, dueDate, now);
        return toDomain(repository.save(entity));
    }

    @Override
    public Task setCompleted(long id, boolean completed, Instant now) {
        var entity = findEntity(id);
        entity.markCompleted(completed, now);
        return toDomain(repository.save(entity));
    }

    @Override
    public void delete(long id) {
        repository.delete(findEntity(id));
    }

    private TaskEntity findEntity(long id) {
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private static Task toDomain(TaskEntity entity) {
        return new Task(
                entity.id(),
                entity.title(),
                entity.description(),
                entity.priority(),
                entity.dueDate(),
                entity.completed(),
                entity.createdAt(),
                entity.updatedAt()
        );
    }
}
