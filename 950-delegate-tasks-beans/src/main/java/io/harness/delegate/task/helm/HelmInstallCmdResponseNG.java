/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.helm;

import io.harness.container.ContainerInfo;
import io.harness.k8s.model.HelmVersion;
import io.harness.logging.CommandExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
public class HelmInstallCmdResponseNG extends HelmCommandResponseNG {
  private List<ContainerInfo> containerInfoList;
  private HelmChartInfo helmChartInfo;
  private int prevReleaseVersion;
  private String releaseName;
  private HelmVersion helmVersion;

  @Builder
  public HelmInstallCmdResponseNG(CommandExecutionStatus commandExecutionStatus, String output,
      List<ContainerInfo> containerInfoList, HelmChartInfo helmChartInfo, int prevReleaseVersion, String releaseName,
      HelmVersion helmVersion) {
    super(commandExecutionStatus, output);
    this.containerInfoList = containerInfoList;
    this.helmChartInfo = helmChartInfo;
    this.prevReleaseVersion = prevReleaseVersion;
    this.releaseName = releaseName;
    this.helmVersion = helmVersion;
  }
}
