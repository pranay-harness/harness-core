package software.wings.beans.infrastructure.instance.info;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public class K8sContainerInfo {
  private String containerId;
  private String name;
  private String image;
}
