package software.wings.beans.artifact;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._959_CG_BEANS)
public enum ArtifactStreamCollectionStatus {
  UNSTABLE,
  STABLE
}
