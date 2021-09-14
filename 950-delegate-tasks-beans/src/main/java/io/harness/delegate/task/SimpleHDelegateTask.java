/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task;

import io.harness.delegate.beans.TaskData;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class SimpleHDelegateTask implements HDelegateTask {
  @NonNull String accountId;
  @NonNull TaskData data;
  @Singular Map<String, String> setupAbstractions;
  String uuid;
  LinkedHashMap<String, String> logStreamingAbstractions;
}
