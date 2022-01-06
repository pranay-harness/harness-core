/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.k8s.K8sCommandUnitConstants.DeleteFailedReleaseResources;
import static io.harness.k8s.K8sCommandUnitConstants.Init;
import static io.harness.k8s.K8sCommandUnitConstants.RecreatePrunedResource;
import static io.harness.k8s.K8sCommandUnitConstants.Rollback;
import static io.harness.k8s.K8sCommandUnitConstants.WaitForSteadyState;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.RUNNING;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.logging.LogLevel.WARN;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.k8s.K8sRollingRollbackBaseHandler;
import io.harness.delegate.k8s.K8sRollingRollbackBaseHandler.ResourceRecreationStatus;
import io.harness.delegate.k8s.beans.K8sRollingRollbackHandlerConfig;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployRollbackTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class K8sRollingDeployRollbackTaskHandler extends K8sTaskHandler {
  @Inject private transient ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private transient K8sTaskHelper k8sTaskHelper;
  @Inject private transient K8sRollingRollbackBaseHandler k8sRollingRollbackBaseHandler;

  private final K8sRollingRollbackHandlerConfig rollbackHandlerConfig = new K8sRollingRollbackHandlerConfig();

  @Override
  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sTaskParameters instanceof K8sRollingDeployRollbackTaskParameters)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sTaskParameters", "Must be instance of K8sRollingDeployRollbackTaskParameters"));
    }

    K8sRollingDeployRollbackTaskParameters request = (K8sRollingDeployRollbackTaskParameters) k8sTaskParameters;

    ExecutionLogCallback initLogCallback = k8sTaskHelper.getExecutionLogCallback(request, Init);
    try {
      init(request, k8sDelegateTaskParams, initLogCallback);
    } catch (Exception e) {
      initLogCallback.saveExecutionLog(getMessage(e), ERROR, FAILURE);
      throw e;
    }

    ResourceRecreationStatus resourceRecreationStatus = ResourceRecreationStatus.NO_RESOURCE_CREATED;

    if (((K8sRollingDeployRollbackTaskParameters) k8sTaskParameters).isPruningEnabled()) {
      ExecutionLogCallback pruneLogCallback = k8sTaskHelper.getExecutionLogCallback(request, RecreatePrunedResource);
      try {
        resourceRecreationStatus = k8sRollingRollbackBaseHandler.recreatePrunedResources(rollbackHandlerConfig,
            request.getReleaseNumber(), request.getPrunedResourcesIds(), pruneLogCallback, k8sDelegateTaskParams);
        logResourceRecreationStatus(resourceRecreationStatus, pruneLogCallback);
      } catch (Exception ex) {
        resourceRecreationStatus = ResourceRecreationStatus.RESOURCE_CREATION_FAILED;
        pruneLogCallback.saveExecutionLog("Failed to recreate pruned resources.", WARN, RUNNING);
        pruneLogCallback.saveExecutionLog(getMessage(ex), WARN, SUCCESS);
      }
    }

    if (((K8sRollingDeployRollbackTaskParameters) k8sTaskParameters).isPruningEnabled()) {
      ExecutionLogCallback deleteLogCallback =
          k8sTaskHelper.getExecutionLogCallback(request, DeleteFailedReleaseResources);
      k8sRollingRollbackBaseHandler.deleteNewResourcesForCurrentFailedRelease(
          rollbackHandlerConfig, request.getReleaseNumber(), deleteLogCallback, k8sDelegateTaskParams);
    }

    boolean success = k8sRollingRollbackBaseHandler.rollback(rollbackHandlerConfig, k8sDelegateTaskParams,
        request.getReleaseNumber(), k8sTaskHelper.getExecutionLogCallback(request, Rollback),
        getResourcesRecreated(request, resourceRecreationStatus), false);
    if (!success) {
      return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    }

    ExecutionLogCallback waitForSteadyStateLogCallback =
        k8sTaskHelper.getExecutionLogCallback(request, WaitForSteadyState);
    try {
      k8sRollingRollbackBaseHandler.steadyStateCheck(rollbackHandlerConfig, k8sDelegateTaskParams,
          request.getTimeoutIntervalInMin(), waitForSteadyStateLogCallback);
      k8sRollingRollbackBaseHandler.postProcess(rollbackHandlerConfig, request.getReleaseName());
      waitForSteadyStateLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
    } catch (Exception e) {
      waitForSteadyStateLogCallback.saveExecutionLog(getMessage(e), ERROR, FAILURE);
      throw e;
    }
  }

  private void logResourceRecreationStatus(
      ResourceRecreationStatus resourceRecreationStatus, ExecutionLogCallback pruneLogCallback) {
    if (resourceRecreationStatus == ResourceRecreationStatus.RESOURCE_CREATION_SUCCESSFUL) {
      pruneLogCallback.saveExecutionLog("Successfully recreated pruned resources.", INFO, SUCCESS);
    } else if (resourceRecreationStatus == ResourceRecreationStatus.NO_RESOURCE_CREATED) {
      pruneLogCallback.saveExecutionLog("No resource recreated.", INFO, SUCCESS);
    }
  }

  @NotNull
  private Set<KubernetesResourceId> getResourcesRecreated(
      K8sRollingDeployRollbackTaskParameters request, ResourceRecreationStatus resourceRecreationStatus) {
    return resourceRecreationStatus.equals(ResourceRecreationStatus.RESOURCE_CREATION_SUCCESSFUL)
        ? new HashSet<>(request.getPrunedResourcesIds())
        : Collections.emptySet();
  }

  private void init(K8sRollingDeployRollbackTaskParameters k8sRollingDeployRollbackTaskParameters,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    rollbackHandlerConfig.setKubernetesConfig(containerDeploymentDelegateHelper.getKubernetesConfig(
        k8sRollingDeployRollbackTaskParameters.getK8sClusterConfig(), false));
    rollbackHandlerConfig.setClient(
        Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath()));

    k8sRollingRollbackBaseHandler.init(
        rollbackHandlerConfig, k8sRollingDeployRollbackTaskParameters.getReleaseName(), executionLogCallback);
  }

  @VisibleForTesting
  K8sRollingRollbackHandlerConfig getRollbackHandlerConfig() {
    return rollbackHandlerConfig;
  }
}
