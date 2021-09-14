/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
  boolean parked;
  String taskType;
  TaskParameters taskParameters;
  String accountId;
  @Singular Map<String, String> taskSetupAbstractions;
  @Singular List<String> taskSelectors;
  Duration executionTimeout;
  String taskDescription;
  LinkedHashMap<String, String> logStreamingAbstractions;
  boolean forceExecute;
}
