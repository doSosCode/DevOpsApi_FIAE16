package de.training.taskapi.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.training.taskapi.domain.Task;
import de.training.taskapi.domain.TaskPriority;
import de.training.taskapi.service.TaskService;
import de.training.taskapi.config.SecurityHeadersFilter;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({TaskController.class, ApiExceptionHandler.class, SecurityHeadersFilter.class})
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @Test
    void returnsTaskByIdAndDefensiveHeaders() throws Exception {
        when(taskService.findById(1L)).thenReturn(new Task(
                1L,
                "Pipeline erklären",
                "CI und CD unterscheiden",
                TaskPriority.MEDIUM,
                LocalDate.of(2030, 1, 1),
                false,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:00Z")
        ));

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(jsonPath("$.title").value("Pipeline erklären"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }

    @Test
    void rejectsInvalidTaskWithoutExposingInternals() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "description": "Test",
                                  "priority": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validierungsfehler"))
                .andExpect(jsonPath("$.fields.title").exists())
                .andExpect(jsonPath("$.fields.priority").exists());
    }

    @Test
    void rejectsExcessiveListLimit() throws Exception {
        mockMvc.perform(get("/api/tasks?limit=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Ungültiger Parameter"));
    }
}
