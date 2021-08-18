package software.wings.sm;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
@Value
@Builder
public class InfraDefinitionSummary {
  private String infraDefinitionId;
  private CloudProviderType cloudProviderType;
  private DeploymentType deploymentType;
  private String cloudProviderName;
  private String displayName;
}
