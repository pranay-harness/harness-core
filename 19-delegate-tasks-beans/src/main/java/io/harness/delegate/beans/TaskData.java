package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldNameConstants(innerTypeName = "TaskDataKeys")
public class TaskData {
  public static final long DEFAULT_SYNC_CALL_TIMEOUT = 60 * 1000;
  public static final long DEFAULT_ASYNC_CALL_TIMEOUT = 10 * 60 * 1000;

  private boolean async;
  @NotNull private String taskType;
  private Object[] parameters;
  private long timeout;
  private int expressionFunctorToken;
}