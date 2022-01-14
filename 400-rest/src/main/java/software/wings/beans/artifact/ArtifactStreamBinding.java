package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
@Data
@Builder
public class ArtifactStreamBinding {
  private String name;
  private List<ArtifactStreamSummary> artifactStreams;
}
