package io.harness.beans;

import io.harness.delegate.task.TaskParameters;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

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
  LinkedHashMap<String, String> logStreamingAbstractions;
}
