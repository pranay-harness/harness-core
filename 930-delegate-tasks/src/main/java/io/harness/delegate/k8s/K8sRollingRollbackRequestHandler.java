/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.k8s.K8sCommandUnitConstants.Init;
import static io.harness.k8s.K8sCommandUnitConstants.Rollback;
import static io.harness.k8s.K8sCommandUnitConstants.WaitForSteadyState;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogColor.Yellow;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.util.Collections.emptySet;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.k8s.beans.K8sRollingRollbackHandlerConfig;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sRollingDeployRollbackResponse;
import io.harness.delegate.task.k8s.K8sRollingRollbackDeployRequest;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.io.IOException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDP)
@NoArgsConstructor
@Slf4j
public class K8sRollingRollbackRequestHandler extends K8sRequestHandler {
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;
  @Inject private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
  @Inject K8sRollingRollbackBaseHandler rollbackBaseHandler;

  private final K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();

  @Override
  protected K8sDeployResponse executeTaskInternal(K8sDeployRequest k8sDeployRequest,
      K8sDelegateTaskParams k8sDelegateTaskParams, ILogStreamingTaskClient logStreamingTaskClient,
      CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(k8sDeployRequest instanceof K8sRollingRollbackDeployRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sDeployRequest", "Must be instance of K8sRollingRollbackDeployRequest"));
    }

    K8sRollingRollbackDeployRequest k8sRollingRollbackDeployRequest =
        (K8sRollingRollbackDeployRequest) k8sDeployRequest;
    LogCallback initLogCallback =
        k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, Init, true, commandUnitsProgress);

    init(k8sRollingRollbackDeployRequest, k8sDelegateTaskParams, initLogCallback);

    rollbackBaseHandler.rollback(rollbackHandlerConfig, k8sDelegateTaskParams,
        k8sRollingRollbackDeployRequest.getReleaseNumber(),
        k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, Rollback, true, commandUnitsProgress), emptySet(),
        true);

    LogCallback waitForSteadyStateLogCallback =
        k8sTaskHelperBase.getLogCallback(logStreamingTaskClient, WaitForSteadyState, true, commandUnitsProgress);

    rollbackBaseHandler.steadyStateCheck(rollbackHandlerConfig, k8sDelegateTaskParams,
        k8sRollingRollbackDeployRequest.getTimeoutIntervalInMin(), waitForSteadyStateLogCallback);
    rollbackBaseHandler.postProcess(rollbackHandlerConfig, k8sRollingRollbackDeployRequest.getReleaseName());
    K8sRollingDeployRollbackResponse response =
        K8sRollingDeployRollbackResponse.builder()
            .k8sPodList(rollbackBaseHandler.getPods(k8sRollingRollbackDeployRequest.getTimeoutIntervalInMin(),
                rollbackHandlerConfig.getPreviousManagedWorkloads(),
                rollbackHandlerConfig.getPreviousCustomManagedWorkloads(), rollbackHandlerConfig.getKubernetesConfig(),
                k8sRollingRollbackDeployRequest.getReleaseName()))
            .build();

    waitForSteadyStateLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return K8sDeployResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .k8sNGTaskResponse(response)
        .build();
  }

  @Override
  public boolean isErrorFrameworkSupported() {
    return true;
  }

  private void init(K8sRollingRollbackDeployRequest rollbackRequest, K8sDelegateTaskParams k8sDelegateTaskParams,
      LogCallback logCallback) throws IOException {
    logCallback.saveExecutionLog("Initializing..\n");
    logCallback.saveExecutionLog(
        color(String.format("Release Name: [%s]", rollbackRequest.getReleaseName()), Yellow, Bold));

    rollbackHandlerConfig.setKubernetesConfig(
        containerDeploymentDelegateBaseHelper.createKubernetesConfig(rollbackRequest.getK8sInfraDelegateConfig()));
    rollbackHandlerConfig.setClient(
        Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath()));

    rollbackBaseHandler.init(rollbackHandlerConfig, rollbackRequest.getReleaseName(), logCallback);
  }

  @VisibleForTesting
  K8sRollingRollbackHandlerConfig getRollbackHandlerConfig() {
    return rollbackHandlerConfig;
  }
}
