package io.harness.beans;

import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class DelegateTaskRequest {
  @Builder.Default boolean parked = false;
  String taskType;
  TaskParameters taskParameters;
  String accountId;
  @Singular Map<String, String> taskSetupAbstractions;
  @Singular List<String> taskSelectors;
  Duration executionTimeout;
  String taskDescription;
}
