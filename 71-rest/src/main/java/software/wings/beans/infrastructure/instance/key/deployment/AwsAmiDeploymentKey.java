package software.wings.beans.infrastructure.instance.key.deployment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class AwsAmiDeploymentKey extends DeploymentKey {
  private String autoScalingGroupName;
}
