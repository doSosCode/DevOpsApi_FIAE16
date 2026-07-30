package de.training.taskapi.api;

import jakarta.validation.constraints.NotNull;

public record TaskCompletionRequest(@NotNull Boolean completed) {
}
