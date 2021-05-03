package io.harness.delegate.task.stepstatus.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CI)
public class DockerArtifactMetadata implements ArtifactMetadataSpec {
  String registryType;
  String registryUrl;
  @Singular List<DockerArtifactDescriptor> dockerArtifacts;
}
