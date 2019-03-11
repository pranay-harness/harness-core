package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskData {
  private Object[] parameters;
  private long timeout;
}