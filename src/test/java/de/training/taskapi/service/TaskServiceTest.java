package de.training.taskapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.training.taskapi.domain.Task;
import de.training.taskapi.domain.TaskPriority;
import de.training.taskapi.domain.TaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Mock
    private TaskRepository taskRepository;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void createNormalizesTextAndDelegatesToRepository() {
        var dueDate = LocalDate.of(2030, 1, 1);
        var expected = new Task(1L, "Test", "Beschreibung", TaskPriority.HIGH, dueDate,
                false, NOW, NOW);
        var service = new TaskService(taskRepository, clock);

        when(taskRepository.create("Test", "Beschreibung", TaskPriority.HIGH, dueDate, NOW))
                .thenReturn(expected);

        var result = service.create("  Test  ", "  Beschreibung  ", TaskPriority.HIGH, dueDate);

        assertThat(result).isEqualTo(expected);
        verify(taskRepository).create("Test", "Beschreibung", TaskPriority.HIGH, dueDate, NOW);
    }

    @Test
    void listLimitIsRestrictedToSafeRange() {
        var service = new TaskService(taskRepository, clock);

        service.findAll(1000);

        verify(taskRepository).findFirst(TaskRepository.MAX_LIST_SIZE);
    }
}
