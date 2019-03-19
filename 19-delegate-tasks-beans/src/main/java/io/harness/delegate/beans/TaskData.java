package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class TaskData {
  @NotNull private String taskType;
  private Object[] parameters;
  private long timeout;
  private int expressionFunctorToken;
}