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

  public static ArtifactStreamSummary fromArtifactStream(ArtifactStream artifactStream) {
    if (artifactStream == null) {
      return null;
    }

    return ArtifactStreamSummary.builder()
        .artifactStreamId(artifactStream.getUuid())
        .settingId(artifactStream.getSettingId())
        .displayName(artifactStream.getName())
        .name(artifactStream.getName())
        .build();
  }
}
