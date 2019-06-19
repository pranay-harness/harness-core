package software.wings.beans.artifact;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArtifactStreamSummary {
  private String artifactStreamId;
  private String displayName;
}
