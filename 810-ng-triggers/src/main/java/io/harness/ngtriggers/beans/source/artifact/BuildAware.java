package io.harness.ngtriggers.beans.source.artifact;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface BuildAware {
  String fetchStageRef();
  String fetchbuildRef();
  String fetchBuildType();
}
