package io.harness.utils;

import io.harness.tasks.Task;
import lombok.Value;

import javax.annotation.Nonnull;

/**
 * The type Dummy task.
 * This is only to provide a Dummy Binding to Guice else it complains while running tests
 */
@Value
public class DummyTask implements Task {
  public static final String TASK_IDENTIFIER = "DUMMY_TASK";

  String uuid;
  String waitId;

  @Override
  @Nonnull
  public String getTaskIdentifier() {
    return TASK_IDENTIFIER;
  }
}
