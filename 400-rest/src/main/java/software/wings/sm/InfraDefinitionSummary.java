package software.wings.sm;

import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InfraDefinitionSummary {
  private String infraDefinitionId;
  private CloudProviderType cloudProviderType;
  private DeploymentType deploymentType;
  private String cloudProviderName;
  private String displayName;
}
