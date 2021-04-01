package io.harness.ngpipeline.pipeline.executions.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.cd.ArtifactSummaryProto;

@OwnedBy(HarnessTeam.CDC)
public interface ArtifactSummary {
  String getType();
  ArtifactSummaryProto toProto();
}
