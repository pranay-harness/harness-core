package software.wings.beans.infrastructure.instance.key.deployment;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class AwsLambdaDeploymentKey extends DeploymentKey {
  private String functionName;
  private String version;
}
