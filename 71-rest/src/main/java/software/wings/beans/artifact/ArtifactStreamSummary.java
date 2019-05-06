package software.wings.beans.artifact;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArtifactStreamSummary {
  private String artifactStreamId;
  private String displayName;
}
