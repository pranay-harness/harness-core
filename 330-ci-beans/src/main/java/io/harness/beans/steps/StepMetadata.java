package io.harness.beans.steps;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StepMetadata {
  private String uuid;
  private int retry;
  private int timeout;
}
