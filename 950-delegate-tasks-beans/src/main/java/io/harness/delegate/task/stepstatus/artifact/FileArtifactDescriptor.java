package io.harness.delegate.task.stepstatus.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CI)
public class FileArtifactDescriptor {
  String name;
  String url;
}
