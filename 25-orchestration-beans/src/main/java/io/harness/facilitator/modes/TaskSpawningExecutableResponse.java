package io.harness.facilitator.modes;

public interface TaskSpawningExecutableResponse extends ExecutableResponse {
  String getTaskId();

  String getTaskIdentifier();
}
