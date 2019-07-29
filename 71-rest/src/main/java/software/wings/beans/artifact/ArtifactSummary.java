package software.wings.beans.artifact;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArtifactSummary {
  private String artifactId;
  private String uiDisplayName;
  private String buildNo;
}
