package io.harness.utils;

import io.harness.tasks.Task;
import lombok.Value;

/**
 * The type Dummy task.
 * This is only to provide a Dummy Binding to Guice else it complains while running tests
 */
@Value
public class DummyTask implements Task {
  String uuid;
  String waitId;
}
