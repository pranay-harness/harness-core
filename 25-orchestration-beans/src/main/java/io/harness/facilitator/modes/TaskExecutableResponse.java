package io.harness.facilitator.modes;

public interface TaskExecutableResponse extends ExecutableResponse {
  String getTaskId();

  String getTaskIdentifier();
}
