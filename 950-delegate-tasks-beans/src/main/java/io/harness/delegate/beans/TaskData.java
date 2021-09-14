/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "TaskDataKeys")
@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public class TaskData {
  public static final long DEFAULT_SYNC_CALL_TIMEOUT = 60 * 1000L;
  public static final long DEFAULT_ASYNC_CALL_TIMEOUT = 10 * 60 * 1000L;

  private boolean parked;
  private boolean async;
  @NotNull private String taskType;
  private Object[] parameters;
  private long timeout;
  private int expressionFunctorToken;
  Map<String, String> expressions;
}
