/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.HarnessStringUtils.join;

import static java.lang.String.format;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.KustomizeCapability;
import io.harness.filesystem.FileIo;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;

@Slf4j
public class KustomizeCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  private static final String KUSTOMIZE_PLUGIN_DIR_SUFFIX = "kustomize/plugin";

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    KustomizeCapability capability = (KustomizeCapability) delegateCapability;
    return CapabilityResponse.builder()
        .validated(doesKustomizePluginDirExist(capability.getPluginRootDir()))
        .delegateCapability(capability)
        .build();
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.KUSTOMIZE_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
    return builder
        .permissionResult(doesKustomizePluginDirExist(parameters.getKustomizeParameters().getPluginRootDir())
                ? PermissionResult.ALLOWED
                : PermissionResult.DENIED)
        .build();
  }

  @VisibleForTesting
  static boolean doesKustomizePluginDirExist(String pluginDir) {
    String kustomizePluginPath = renderPathUsingEnvVariables(pluginDir);
    if (isNotEmpty(kustomizePluginPath)) {
      try {
        kustomizePluginPath = join("/", kustomizePluginPath, KUSTOMIZE_PLUGIN_DIR_SUFFIX);
        return FileIo.checkIfFileExist(kustomizePluginPath);
      } catch (IOException e) {
        return false;
      }
    }
    return true;
  }

  private static String renderPathUsingEnvVariables(String kustomizePluginPath) {
    if (isNotEmpty(kustomizePluginPath)) {
      try {
        return executeShellCommand(format("echo \"%s\"", kustomizePluginPath));
      } catch (Exception ex) {
        log.error(format("Could not echo kustomizePluginPath %s", kustomizePluginPath));
      }
    }
    return kustomizePluginPath;
  }

  private static String executeShellCommand(String cmd) throws InterruptedException, TimeoutException, IOException {
    return new ProcessExecutor()
        .command("/bin/sh", "-c", cmd)
        .readOutput(true)
        .timeout(5, TimeUnit.SECONDS)
        .execute()
        .outputUTF8()
        .trim();
  }
}
