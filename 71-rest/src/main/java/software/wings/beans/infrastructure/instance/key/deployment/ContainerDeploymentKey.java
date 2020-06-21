package software.wings.beans.infrastructure.instance.key.deployment;

import io.harness.mongo.index.Indexed;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.container.Label;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ContainerDeploymentKey extends DeploymentKey {
  private String containerServiceName;
  @Indexed private List<Label> labels;
  private String newVersion;
}
