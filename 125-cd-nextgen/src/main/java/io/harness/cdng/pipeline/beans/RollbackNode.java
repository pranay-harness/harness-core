package io.harness.cdng.pipeline.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RollbackNode {
  String nodeId;
  String dependentNodeIdentifier;
  boolean shouldAlwaysRun;
}
