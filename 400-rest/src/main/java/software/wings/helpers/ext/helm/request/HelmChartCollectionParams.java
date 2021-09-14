/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.helpers.ext.helm.request;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.manifests.request.ManifestCollectionParams;
import io.harness.expression.ExpressionEvaluator;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDC)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class HelmChartCollectionParams implements ManifestCollectionParams {
  private String accountId;
  private String appId;
  private String appManifestId;
  private String serviceId;
  private HelmChartConfigParams helmChartConfigParams;
  private Set<String> publishedVersions;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return helmChartConfigParams.fetchRequiredExecutionCapabilities(maskingEvaluator);
  }
}
