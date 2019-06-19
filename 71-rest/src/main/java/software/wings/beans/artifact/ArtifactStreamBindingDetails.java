package software.wings.beans.artifact;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ArtifactStreamBindingDetails {
  private String name;
  private List<ArtifactStreamSummary> artifactStreams;
}
