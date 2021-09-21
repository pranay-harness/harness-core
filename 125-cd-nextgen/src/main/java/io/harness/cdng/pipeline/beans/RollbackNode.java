package io.harness.cdng.pipeline.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@RecasterAlias("io.harness.cdng.pipeline.beans.RollbackNode")
@OwnedBy(HarnessTeam.CDP)
public class RollbackNode {
  String nodeId;
  String dependentNodeIdentifier;
  boolean shouldAlwaysRun;
}
