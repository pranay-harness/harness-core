package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.BuildExecutionSummary;
import software.wings.beans.artifact.Artifact;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
public class ArtifactMetadata {
  String artifactSource;
  String buildNo;

  static List<ArtifactMetadata> fromArtifacts(List<Artifact> artifacts) {
    return MetadataUtils.map(artifacts, ArtifactMetadata::fromArtifact);
  }

  static List<ArtifactMetadata> fromBuildExecutionSummaries(List<BuildExecutionSummary> buildExecutionSummaries) {
    return MetadataUtils.map(buildExecutionSummaries, ArtifactMetadata::fromBuildExecutionSummary);
  }

  private static ArtifactMetadata fromArtifact(Artifact artifact) {
    if (artifact == null) {
      return null;
    }

    return ArtifactMetadata.builder()
        .artifactSource(artifact.getArtifactSourceName())
        .buildNo(artifact.getUiDisplayName())
        .build();
  }

  private static ArtifactMetadata fromBuildExecutionSummary(BuildExecutionSummary buildExecutionSummary) {
    if (buildExecutionSummary == null) {
      return null;
    }

    return ArtifactMetadata.builder()
        .artifactSource(buildExecutionSummary.getArtifactSource())
        .buildNo(buildExecutionSummary.getBuildName())
        .build();
  }
}
