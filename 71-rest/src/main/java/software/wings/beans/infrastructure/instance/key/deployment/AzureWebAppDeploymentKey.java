package software.wings.beans.infrastructure.instance.key.deployment;

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
public class AzureWebAppDeploymentKey extends DeploymentKey {
  private String appName;
  private String slotName;
}
