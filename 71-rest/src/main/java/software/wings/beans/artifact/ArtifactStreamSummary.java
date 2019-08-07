package software.wings.beans.artifact;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArtifactStreamSummary {
  private String artifactStreamId;
  private String settingId;
  private String displayName;
  private String name;
  private String lastCollectedArtifact;

  public static ArtifactStreamSummary fromArtifactStream(
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
