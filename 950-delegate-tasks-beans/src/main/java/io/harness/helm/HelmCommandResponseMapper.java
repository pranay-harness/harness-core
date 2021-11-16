package io.harness.helm;

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
}