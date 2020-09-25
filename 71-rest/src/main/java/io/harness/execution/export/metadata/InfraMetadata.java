package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.ElementExecutionSummary;
import software.wings.sm.InfraDefinitionSummary;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
public class InfraMetadata {
  String name;
  String cloudProviderName;
  String cloudProviderType;
  String deploymentType;

  static List<InfraMetadata> fromElementExecutionSummary(ElementExecutionSummary executionSummary) {
    if (executionSummary == null) {
      return null;
    }

    return MetadataUtils.map(executionSummary.getInfraDefinitionSummaries(), InfraMetadata::fromInfraDefinitionSummary);
  }

  private static InfraMetadata fromInfraDefinitionSummary(InfraDefinitionSummary summary) {
    if (summary == null) {
      return null;
    }

    return InfraMetadata.builder()
        .name(summary.getDisplayName())
        .cloudProviderName(summary.getCloudProviderName())
        .cloudProviderType(summary.getCloudProviderType() == null ? null : summary.getCloudProviderType().name())
        .deploymentType(summary.getDeploymentType() == null ? null : summary.getDeploymentType().getDisplayName())
        .build();
  }
}
