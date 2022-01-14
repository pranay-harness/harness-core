package io.harness.app.beans.entities;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BuildHealth {
  private long count;
  private double rate;
}
