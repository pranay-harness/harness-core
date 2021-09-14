/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;
import io.harness.delegate.task.helm.HelmChartInfo;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("k8sHelmDeploymentElement")
@OwnedBy(CDP)
public class K8sHelmDeploymentElement implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "k8sHelmDeploymentInfo";

  private HelmChartInfo previousDeployedHelmChart;

  @Override
  public String getType() {
    return "k8sHelmDeploymentElement";
  }
}
