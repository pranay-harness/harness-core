/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
