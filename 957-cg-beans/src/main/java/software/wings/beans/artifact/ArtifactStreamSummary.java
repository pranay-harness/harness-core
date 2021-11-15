package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
@Value
@Builder
public class ArtifactStreamSummary {
  private String artifactStreamId;
  private String settingId;
  private String displayName;
  private String name;
  private String lastCollectedArtifact;
  private ArtifactSummary defaultArtifact;
}
