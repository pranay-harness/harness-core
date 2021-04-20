package software.wings.beans.infrastructure.instance.key.deployment;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
public class AwsLambdaDeploymentKey extends DeploymentKey {
  private String functionName;
  private String version;
}
