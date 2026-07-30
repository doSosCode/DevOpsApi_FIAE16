package de.training.taskapi.api;

import de.training.taskapi.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@Validated
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Begrenzt die Ergebnisgröße bewusst, damit eine wachsende Tabelle nicht
     * unbegrenzt in den Arbeitsspeicher geladen wird.
     */
    @GetMapping
    public List<TaskResponse> findAll(
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit
    ) {
        return taskService.findAll(limit).stream().map(TaskResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TaskResponse findById(@PathVariable @Positive long id) {
        return TaskResponse.from(taskService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        var created = taskService.create(
                request.title(), request.description(), request.priority(), request.dueDate()
        );
        return ResponseEntity.created(URI.create("/api/tasks/" + created.id()))
                .body(TaskResponse.from(created));
    }

    @PutMapping("/{id}")
    public TaskResponse update(
            @PathVariable @Positive long id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return TaskResponse.from(taskService.update(
                id, request.title(), request.description(), request.priority(), request.dueDate()
        ));
    }

    @PatchMapping("/{id}/completion")
    public TaskResponse setCompleted(
            @PathVariable @Positive long id,
            @Valid @RequestBody TaskCompletionRequest request
    ) {
        return TaskResponse.from(taskService.setCompleted(id, request.completed()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
