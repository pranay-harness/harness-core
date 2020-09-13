package io.harness.states;

import static io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo.CALLBACK_IDS;
import static software.wings.beans.TaskType.CI_LE_STATUS;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.beans.sweepingoutputs.StepTaskDetails;
import io.harness.cdng.orchestration.StepUtils;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.HDelegateTask;
import io.harness.delegate.task.stepstatus.StepStatusTaskParameters;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.status.Status;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.managerclient.ManagerCIResource;
import io.harness.plancreators.IntegrationStagePlanCreator;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.stateutils.buildstate.BuildSetupUtils;
import io.harness.tasks.TaskExecutor;
import io.harness.tasks.TaskMode;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * This state will setup the build environment, clone the git repository for running CI job.
 */

@Slf4j
public class LiteEngineTaskStep implements Step, SyncExecutable<LiteEngineTaskStepInfo> {
  @Inject private ManagerCIResource managerCIResource;
  @Inject private BuildSetupUtils buildSetupUtils;
  @Inject private Map<String, TaskExecutor<HDelegateTask> > taskExecutorMap;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;
  public static final StepType STEP_TYPE = LiteEngineTaskStepInfo.typeInfo.getStepType();

  // TODO Async can not be supported at this point. We have to build polling framework on CI manager.
  //     Async will be supported once we will have delegate microservice ready.

  @Override
  public StepResponse executeSync(Ambiance ambiance, LiteEngineTaskStepInfo liteEngineTaskStepInfo,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    try {
      // TODO Handle response and fetch cluster from input element
      addCallBackIds(liteEngineTaskStepInfo, ambiance);
      buildSetupUtils.executeCILiteEngineTask(liteEngineTaskStepInfo, ambiance);
      return StepResponse.builder().status(Status.SUCCEEDED).build();
    } catch (Exception e) {
      logger.error("state execution failed", e);
    }
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  private void addCallBackIds(LiteEngineTaskStepInfo liteEngineTaskStepInfo, Ambiance ambiance) {
    Map<String, String> taskIds = new HashMap<>();
    liteEngineTaskStepInfo.getSteps().getSteps().forEach(
        executionWrapper -> { addCallBackId(executionWrapper, ambiance, taskIds); });

    executionSweepingOutputResolver.consume(ambiance, CALLBACK_IDS, StepTaskDetails.builder().taskIds(taskIds).build(),
        IntegrationStagePlanCreator.GROUP_NAME);
  }

  private void addCallBackId(ExecutionWrapper executionWrapper, Ambiance ambiance, Map<String, String> taskIds) {
    TaskExecutor<HDelegateTask> executor = taskExecutorMap.get(TaskMode.DELEGATE_TASK_V3.name());
    final String accountId = ambiance.getSetupAbstractions().get("accountId");

    if (executionWrapper != null) {
      if (executionWrapper instanceof StepElement) {
        StepElement stepElement = (StepElement) executionWrapper;
        setCallBackIdInStepInfo(ambiance, stepElement, accountId, executor, taskIds);
      } else if (executionWrapper instanceof ParallelStepElement) {
        ParallelStepElement parallel = (ParallelStepElement) executionWrapper;
        parallel.getSections().forEach(section -> addCallBackId(section, ambiance, taskIds));
      } else {
        throw new InvalidRequestException("Only Parallel or StepElement is supported");
      }
    }
  }
  private void setCallBackIdInStepInfo(Ambiance ambiance, StepElement stepElement, String accountId,
      TaskExecutor<HDelegateTask> executor, Map<String, String> taskIds) {
    // TODO replace identifier as key in case two steps can have same identifier

    if (stepElement.getType().equals("run")) {
      RunStepInfo runStepInfo = (RunStepInfo) stepElement.getStepSpecType();
      String taskId = queueDelegateTask(ambiance, runStepInfo.getTimeout(), accountId, executor);
      runStepInfo.setCallbackId(taskId);
      taskIds.put(runStepInfo.getIdentifier(), taskId);
    }

    if (stepElement.getType().equals("publishArtifacts")) {
      PublishStepInfo publishStepInfo = (PublishStepInfo) stepElement.getStepSpecType();
      String taskId = queueDelegateTask(ambiance, publishStepInfo.getTimeout(), accountId, executor);
      publishStepInfo.setCallbackId(taskId);
      taskIds.put(publishStepInfo.getIdentifier(), taskId);
    }

    if (stepElement.getType().equals("saveCache")) {
      SaveCacheStepInfo saveCacheStepInfo = (SaveCacheStepInfo) stepElement.getStepSpecType();
      String taskId = queueDelegateTask(ambiance, saveCacheStepInfo.getTimeout(), accountId, executor);
      saveCacheStepInfo.setCallbackId(taskId);
      taskIds.put(saveCacheStepInfo.getIdentifier(), taskId);
    }

    if (stepElement.getType().equals("restoreCache")) {
      RestoreCacheStepInfo restoreCacheStepInfo = (RestoreCacheStepInfo) stepElement.getStepSpecType();
      String taskId = queueDelegateTask(ambiance, restoreCacheStepInfo.getTimeout(), accountId, executor);
      restoreCacheStepInfo.setCallbackId(taskId);
      taskIds.put(restoreCacheStepInfo.getIdentifier(), taskId);
    }
  }

  private String queueDelegateTask(
      Ambiance ambiance, long timeout, String accountId, TaskExecutor<HDelegateTask> executor) {
    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .parked(true)
                                  .taskType(CI_LE_STATUS.name())
                                  .parameters(new Object[] {StepStatusTaskParameters.builder().build()})
                                  .timeout(timeout)
                                  .build();

    HDelegateTask task =
        (HDelegateTask) StepUtils.prepareDelegateTaskInput(accountId, taskData, ambiance.getSetupAbstractions());

    return executor.queueTask(ambiance.getSetupAbstractions(), task);
  }
}
