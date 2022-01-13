/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.helpers.ext.helm.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.container.ContainerInfo;
import io.harness.delegate.task.helm.HelmChartInfo;
import io.harness.delegate.task.helm.HelmCommandResponse;
import io.harness.logging.CommandExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by anubhaw on 4/2/18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@OwnedBy(CDP)
public class HelmInstallCommandResponse extends HelmCommandResponse {
  private List<ContainerInfo> containerInfoList;
  private io.harness.delegate.task.helm.HelmChartInfo helmChartInfo;

  @Builder
  public HelmInstallCommandResponse(CommandExecutionStatus commandExecutionStatus, String output,
      List<ContainerInfo> containerInfoList, HelmChartInfo helmChartInfo) {
    super(commandExecutionStatus, output);
    this.containerInfoList = containerInfoList;
    this.helmChartInfo = helmChartInfo;
  }
}
