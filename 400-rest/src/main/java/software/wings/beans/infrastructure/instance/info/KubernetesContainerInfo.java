/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.infrastructure.instance.info;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.task.helm.HelmChartInfo;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author rktummala on 09/05/17
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public class KubernetesContainerInfo extends ContainerInfo {
  private String controllerType;
  private String controllerName;
  private String serviceName;
  private String podName;
  private String ip;
  private String namespace;

  /*
  Helm Release to which the kubernetes pods belong to
   */
  private String releaseName;

  // only applicable for helm deployments
  private HelmChartInfo helmChartInfo;

  @Builder
  public KubernetesContainerInfo(String clusterName, String controllerType, String controllerName, String serviceName,
      String podName, String ip, String namespace, HelmChartInfo helmChartInfo, String releaseName) {
    super(clusterName);
    this.controllerType = controllerType;
    this.controllerName = controllerName;
    this.serviceName = serviceName;
    this.podName = podName;
    this.ip = ip;
    this.namespace = namespace;
    this.helmChartInfo = helmChartInfo;
    this.releaseName = releaseName;
  }
}
