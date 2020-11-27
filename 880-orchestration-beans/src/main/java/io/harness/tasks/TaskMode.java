package io.harness.tasks;

public enum TaskMode {
  NOOP,
  // Conforms to Delegate 1.0
  DELEGATE_TASK_V1,
  // Conforms to NgDelegate where manager is used as a delegate service
  DELEGATE_TASK_V2,
  // For Delegate 2.0
  DELEGATE_TASK_V3,
}
