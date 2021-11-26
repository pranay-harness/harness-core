package io.harness.helm;

import io.harness.delegate.task.helm.HelmInstallCmdResponseNG;

import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;

import lombok.experimental.UtilityClass;

@UtilityClass
public class HelmCommandResponseMapper {
  public HelmInstallCommandResponse getHelmInstallCommandResponse(HelmClientImpl.HelmCliResponse cliResponse) {
    return HelmInstallCommandResponse.builder()
        .commandExecutionStatus(cliResponse.getCommandExecutionStatus())
        .output(cliResponse.getOutput())
        .build();
  }

  public HelmInstallCmdResponseNG getHelmInstCmdRespNG(HelmClientImpl.HelmCliResponse cliResponse) {
    return HelmInstallCmdResponseNG.builder()
        .commandExecutionStatus(cliResponse.getCommandExecutionStatus())
        .output(cliResponse.getOutput())
        .build();
  }
}