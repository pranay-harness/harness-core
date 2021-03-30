package io.harness.states;

import static io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo.CALLBACK_IDS;
import static io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo.LOG_KEYS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.lang.String.format;

import io.harness.beans.dependencies.ServiceDependency;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.outcomes.DependencyOutcome;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.sweepingoutputs.StepLogKeyDetails;
import io.harness.beans.sweepingoutputs.StepTaskDetails;
import io.harness.ci.integrationstage.IntegrationStageUtils;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.ci.CIBuildSetupTaskParams;
import io.harness.delegate.beans.ci.k8s.CIContainerStatus;
import io.harness.delegate.beans.ci.k8s.CiK8sTaskResponse;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.task.HDelegateTask;
import io.harness.delegate.task.stepstatus.StepStatusTaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.model.ImageDetails;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logstreaming.LogStreamingHelper;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.steps.ParallelStepElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.serializer.KryoSerializer;
import io.harness.stateutils.buildstate.BuildSetupUtils;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.StepUtils;
import io.harness.yaml.core.timeout.TimeoutUtils;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * This state will setup the build environment, clone the git repository for running CI job.
 */

@Slf4j
public class LiteEngineTaskStep implements TaskExecutable<LiteEngineTaskStepInfo, K8sTaskExecutionResponse> {
  public static final String TASK_TYPE_CI_BUILD = "CI_BUILD";
  public static final String LE_STATUS_TASK_TYPE = "CI_LE_STATUS";
  @Inject private BuildSetupUtils buildSetupUtils;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private CIDelegateTaskExecutor ciDelegateTaskExecutor;

  private static final String DEPENDENCY_OUTCOME = "dependencies";
  public static final StepType STEP_TYPE = LiteEngineTaskStepInfo.STEP_TYPE;

  @Override
  public Class<LiteEngineTaskStepInfo> getStepParametersClass() {
    return LiteEngineTaskStepInfo.class;
  }

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, LiteEngineTaskStepInfo stepParameters, StepInputPackage inputPackage) {
    Map<String, String> taskIds = addCallBackIds(stepParameters, ambiance);
    String logPrefix = getLogPrefix(ambiance);
    Map<String, String> stepLogKeys = getStepLogKeys(stepParameters, ambiance, logPrefix);

    CIBuildSetupTaskParams buildSetupTaskParams =
        buildSetupUtils.getBuildSetupTaskParams(stepParameters, ambiance, taskIds, logPrefix, stepLogKeys);
    log.info("Created params for build task: {}", buildSetupTaskParams);

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(stepParameters.getTimeout())
                                  .taskType(TASK_TYPE_CI_BUILD)
                                  .parameters(new Object[] {buildSetupTaskParams})
                                  .build();

    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer);
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, LiteEngineTaskStepInfo stepParameters, Supplier<K8sTaskExecutionResponse> responseSupplier) {
    K8sTaskExecutionResponse k8sTaskExecutionResponse = responseSupplier.get();

    DependencyOutcome dependencyOutcome =
        getDependencyOutcome(ambiance, stepParameters, k8sTaskExecutionResponse.getK8sTaskResponse());
    StepResponse.StepOutcome stepOutcome =
        StepResponse.StepOutcome.builder().name(DEPENDENCY_OUTCOME).outcome(dependencyOutcome).build();
    if (k8sTaskExecutionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      log.info(
          "LiteEngineTaskStep pod creation task executed successfully with response [{}]", k8sTaskExecutionResponse);
      return StepResponse.builder().status(Status.SUCCEEDED).stepOutcome(stepOutcome).build();

    } else {
      log.error("LiteEngineTaskStep execution finished with status [{}] and response [{}]",
          k8sTaskExecutionResponse.getCommandExecutionStatus(), k8sTaskExecutionResponse);

      StepResponseBuilder stepResponseBuilder = StepResponse.builder().status(Status.FAILED).stepOutcome(stepOutcome);
      if (k8sTaskExecutionResponse.getErrorMessage() != null) {
        stepResponseBuilder.failureInfo(
            FailureInfo.newBuilder().setErrorMessage(k8sTaskExecutionResponse.getErrorMessage()).build());
      }
      return stepResponseBuilder.build();
    }
  }

  private DependencyOutcome getDependencyOutcome(
      Ambiance ambiance, LiteEngineTaskStepInfo stepParameters, CiK8sTaskResponse ciK8sTaskResponse) {
    List<ContainerDefinitionInfo> serviceContainers = buildSetupUtils.getBuildServiceContainers(stepParameters);
    List<ServiceDependency> serviceDependencyList = new ArrayList<>();
    if (serviceContainers == null) {
      return DependencyOutcome.builder().serviceDependencyList(serviceDependencyList).build();
    }

    Map<String, CIContainerStatus> containerStatusMap = new HashMap<>();
    if (ciK8sTaskResponse != null && ciK8sTaskResponse.getPodStatus() != null
        && ciK8sTaskResponse.getPodStatus().getCiContainerStatusList() != null) {
      for (CIContainerStatus containerStatus : ciK8sTaskResponse.getPodStatus().getCiContainerStatusList()) {
        containerStatusMap.put(containerStatus.getName(), containerStatus);
      }
    }

    String logPrefix = getLogPrefix(ambiance);
    for (ContainerDefinitionInfo serviceContainer : serviceContainers) {
      String logKey = format("%s/serviceId:%s", logPrefix, serviceContainer.getStepIdentifier());
      String containerName = serviceContainer.getName();
      if (containerStatusMap.containsKey(containerName)) {
        CIContainerStatus containerStatus = containerStatusMap.get(containerName);

        ServiceDependency.Status status = ServiceDependency.Status.SUCCESS;
        if (containerStatus.getStatus() == CIContainerStatus.Status.ERROR) {
          status = ServiceDependency.Status.ERROR;
        }
        serviceDependencyList.add(ServiceDependency.builder()
                                      .identifier(serviceContainer.getStepIdentifier())
                                      .name(serviceContainer.getStepName())
                                      .image(containerStatus.getImage())
                                      .startTime(containerStatus.getStartTime())
                                      .endTime(containerStatus.getEndTime())
                                      .errorMessage(containerStatus.getErrorMsg())
                                      .status(status)
                                      .logKeys(Collections.singletonList(logKey))
                                      .build());
      } else {
        ImageDetails imageDetails = serviceContainer.getContainerImageDetails().getImageDetails();
        String image = imageDetails.getName();
        if (isEmpty(imageDetails.getTag())) {
          image += format(":%s", imageDetails.getTag());
        }
        serviceDependencyList.add(ServiceDependency.builder()
                                      .identifier(serviceContainer.getStepIdentifier())
                                      .name(serviceContainer.getStepName())
                                      .image(image)
                                      .errorMessage("Unknown")
                                      .status(ServiceDependency.Status.ERROR)
                                      .logKeys(Collections.singletonList(logKey))
                                      .build());
      }
    }
    return DependencyOutcome.builder().serviceDependencyList(serviceDependencyList).build();
  }

  Map<String, String> addCallBackIds(LiteEngineTaskStepInfo liteEngineTaskStepInfo, Ambiance ambiance) {
    Map<String, String> taskIds = new HashMap<>();
    liteEngineTaskStepInfo.getExecutionElementConfig().getSteps().forEach(
        executionWrapper -> addCallBackId(executionWrapper, ambiance, taskIds));

    executionSweepingOutputResolver.consume(
        ambiance, CALLBACK_IDS, StepTaskDetails.builder().taskIds(taskIds).build(), StepOutcomeGroup.STAGE.name());
    return taskIds;
  }

  private void addCallBackId(ExecutionWrapperConfig executionWrapper, Ambiance ambiance, Map<String, String> taskIds) {
    final String accountId = ambiance.getSetupAbstractionsMap().get("accountId");

    if (executionWrapper != null) {
      if (executionWrapper.getStep() != null && !executionWrapper.getStep().isNull()) {
        StepElementConfig stepElementConfig = IntegrationStageUtils.getStepElementConfig(executionWrapper);
        createStepCallbackIds(ambiance, stepElementConfig, accountId, taskIds);
      } else if (executionWrapper.getParallel() != null && !executionWrapper.getParallel().isNull()) {
        ParallelStepElementConfig parallelStepElementConfig =
            IntegrationStageUtils.getParallelStepElementConfig(executionWrapper);
        parallelStepElementConfig.getSections().forEach(section -> addCallBackId(section, ambiance, taskIds));
      } else {
        throw new InvalidRequestException("Only Parallel or StepElement is supported");
      }
    }
  }

  private void createStepCallbackIds(
      Ambiance ambiance, StepElementConfig stepElement, String accountId, Map<String, String> taskIds) {
    // TODO replace identifier as key in case two steps can have same identifier
    long timeout = TimeoutUtils.getTimeoutInSeconds(
        stepElement.getTimeout(), ((CIStepInfo) stepElement.getStepSpecType()).getDefaultTimeout());
    String taskId = queueDelegateTask(ambiance, timeout, accountId, ciDelegateTaskExecutor);
    taskIds.put(stepElement.getIdentifier(), taskId);
  }

  private String queueDelegateTask(Ambiance ambiance, long timeout, String accountId, CIDelegateTaskExecutor executor) {
    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .parked(true)
                                  .taskType(LE_STATUS_TASK_TYPE)
                                  .parameters(new Object[] {StepStatusTaskParameters.builder().build()})
                                  .timeout(timeout)
                                  .build();

    HDelegateTask task =
        (HDelegateTask) StepUtils.prepareDelegateTaskInput(accountId, taskData, ambiance.getSetupAbstractionsMap());

    return executor.queueTask(ambiance.getSetupAbstractionsMap(), task);
  }

  private Map<String, String> getStepLogKeys(
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, Ambiance ambiance, String logPrefix) {
    Map<String, String> logKeyByStepId = new HashMap<>();
    liteEngineTaskStepInfo.getExecutionElementConfig().getSteps().forEach(
        executionWrapper -> addLogKey(executionWrapper, logPrefix, logKeyByStepId));

    Map<String, List<String>> logKeys = new HashMap<>();
    logKeyByStepId.forEach((stepId, logKey) -> logKeys.put(stepId, Collections.singletonList(logKey)));
    executionSweepingOutputResolver.consume(
        ambiance, LOG_KEYS, StepLogKeyDetails.builder().logKeys(logKeys).build(), StepOutcomeGroup.STAGE.name());
    return logKeyByStepId;
  }

  private void addLogKey(
      ExecutionWrapperConfig executionWrapper, String logPrefix, Map<String, String> logKeyByStepId) {
    if (executionWrapper != null) {
      if (executionWrapper.getStep() != null && !executionWrapper.getStep().isNull()) {
        StepElementConfig stepElementConfig = IntegrationStageUtils.getStepElementConfig(executionWrapper);

        logKeyByStepId.put(stepElementConfig.getIdentifier(), getStepLogKey(stepElementConfig, logPrefix));
      } else if (executionWrapper.getParallel() != null && !executionWrapper.getParallel().isNull()) {
        ParallelStepElementConfig parallelStepElementConfig =
            IntegrationStageUtils.getParallelStepElementConfig(executionWrapper);
        parallelStepElementConfig.getSections().forEach(section -> addLogKey(section, logPrefix, logKeyByStepId));
      } else {
        throw new InvalidRequestException("Only Parallel or StepElement is supported");
      }
    }
  }

  private String getStepLogKey(StepElementConfig stepElement, String logPrefix) {
    return format("%s/stepId:%s", logPrefix, stepElement.getIdentifier());
  }

  private String getLogPrefix(Ambiance ambiance) {
    LinkedHashMap<String, String> logAbstractions = StepUtils.generateStageLogAbstractions(ambiance);
    return LogStreamingHelper.generateLogBaseKey(logAbstractions);
  }
}
