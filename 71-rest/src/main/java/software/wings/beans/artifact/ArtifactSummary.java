package software.wings.beans.artifact;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArtifactSummary {
  private String uuid;
  private String uiDisplayName;
  private String buildNo;

  public static ArtifactSummary prepareSummaryFromArtifact(Artifact artifact) {
    if (artifact == null) {
      return null;
    }

    return ArtifactSummary.builder()
        .uuid(artifact.getUuid())
        .uiDisplayName(artifact.getUiDisplayName())
        .buildNo(artifact.getBuildNo())
        .build();
  }
}
