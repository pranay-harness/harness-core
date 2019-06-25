package software.wings.beans.artifact;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ArtifactStreamBinding {
  private String name;
  private List<ArtifactStreamSummary> artifactStreams;
}
