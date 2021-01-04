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
public class AwsCodeDeployDeploymentKey extends DeploymentKey {
  // This is Revision Localtion, S3 artifact link
  private String key;
}
