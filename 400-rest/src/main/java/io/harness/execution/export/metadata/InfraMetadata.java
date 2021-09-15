/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.ElementExecutionSummary;
import software.wings.sm.InfraDefinitionSummary;

import java.util.List;
import lombok.Builder;
import lombok.Value;

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
