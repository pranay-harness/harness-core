/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.executioncapability;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ProcessExecutorCapability;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

@Singleton
@Slf4j
public class ProcessExecutorCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    ProcessExecutorCapability processExecutorCapability = (ProcessExecutorCapability) delegateCapability;

    ProcessExecutor processExecutor =
        new ProcessExecutor().command(processExecutorCapability.getProcessExecutorArguments());
    boolean valid = false;
    try {
      final ProcessResult result = processExecutor.execute();
      valid = result.getExitValue() == 0;
    } catch (Exception e) {
      StringBuilder msg = new StringBuilder(128).append("Failed to execute command with arguments, ");
      processExecutorCapability.getProcessExecutorArguments().forEach(capability -> msg.append(capability).append(' '));
      log.error(msg.toString());
    }

    return CapabilityResponse.builder().delegateCapability(processExecutorCapability).validated(valid).build();
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.PROCESS_EXECUTOR_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }

    ProcessExecutor processExecutor =
        new ProcessExecutor().command(parameters.getProcessExecutorParameters().getArgsList());
    boolean valid = false;
    try {
      final ProcessResult result = processExecutor.execute();
      valid = result.getExitValue() == 0;
    } catch (Exception e) {
      log.error("Capability check failed to execute command with arguments, "
          + String.join(" ", parameters.getProcessExecutorParameters().getArgsList()));
    }
    return builder.permissionResult(valid ? PermissionResult.ALLOWED : PermissionResult.DENIED).build();
  }
}
