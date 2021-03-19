package io.harness.pms.sdk.core.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OptionalSweepingOutput {
  boolean found;
  String output;
}
