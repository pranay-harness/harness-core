/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.helm.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
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
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
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
