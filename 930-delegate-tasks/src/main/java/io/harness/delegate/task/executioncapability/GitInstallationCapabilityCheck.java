/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

@Slf4j
public class GitInstallationCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  private static final String GIT_HELP_COMMAND = "git --help";
  private static final int GIT_COMMAND_TIMEOUT_MILLIS = 1000;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    return CapabilityResponse.builder()
        .validated(checkIfGitClientIsInstalled())
        .delegateCapability(delegateCapability)
        .build();
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.GIT_INSTALLATION_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
    return builder.permissionResult(checkIfGitClientIsInstalled() ? PermissionResult.ALLOWED : PermissionResult.DENIED)
        .build();
  }

  boolean checkIfGitClientIsInstalled() {
    boolean gitInstalled = false;
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .command("/bin/sh", "-c", GIT_HELP_COMMAND)
                                          .timeout(GIT_COMMAND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    try {
      ProcessResult processResult = processExecutor.execute();
      if (processResult.getExitValue() == 0) {
        return true;
      }
    } catch (IOException | TimeoutException e) {
      log.error("Exception occurred while running git --help command", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted while running git --help command", e);
    }
    return gitInstalled;
  }
}
