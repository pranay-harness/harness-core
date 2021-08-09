package software.wings.beans.infrastructure.instance.key.deployment;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.mongo.index.FdIndex;

import software.wings.beans.container.Label;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
@TargetModule(HarnessModule._959_CG_BEANS)
public class ContainerDeploymentKey extends DeploymentKey {
  private String containerServiceName;
  @FdIndex private List<Label> labels;
  private String newVersion;
}
