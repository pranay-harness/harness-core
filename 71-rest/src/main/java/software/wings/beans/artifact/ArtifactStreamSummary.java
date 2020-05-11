package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class ArtifactStreamSummary {
  private String artifactStreamId;
  private String settingId;
  private String displayName;
  private String name;
  private String lastCollectedArtifact;
  private ArtifactSummary defaultArtifact;

  public static ArtifactStreamSummary prepareSummaryFromArtifactStream(
      ArtifactStream artifactStream, Artifact lastCollectedArtifact) {
    if (artifactStream == null) {
      return null;
    }

    String lastCollectedArtifactName = null;
    if (lastCollectedArtifact != null) {
      lastCollectedArtifactName = lastCollectedArtifact.getBuildNo();
    }
    return ArtifactStreamSummary.builder()
        .artifactStreamId(artifactStream.getUuid())
        .settingId(artifactStream.getSettingId())
        .displayName(artifactStream.getName())
        .lastCollectedArtifact(lastCollectedArtifactName)
        .name(artifactStream.getName())
        .build();
  }
}
