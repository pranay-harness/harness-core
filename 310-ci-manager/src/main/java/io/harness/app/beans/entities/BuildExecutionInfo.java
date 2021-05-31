package io.harness.app.beans.entities;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BuildExecutionInfo {
  private long time;
  private BuildCount builds;
}
