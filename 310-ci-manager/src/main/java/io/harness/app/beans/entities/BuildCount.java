package io.harness.app.beans.entities;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BuildCount {
  private long total;
  private long success;
  private long failed;
  private long aborted;
  private long expired;
}
