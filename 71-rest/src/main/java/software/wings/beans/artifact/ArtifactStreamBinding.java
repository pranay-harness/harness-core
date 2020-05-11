package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@OwnedBy(CDC)
@Data
@Builder
public class ArtifactStreamBinding {
  private String name;
  private List<ArtifactStreamSummary> artifactStreams;
}
