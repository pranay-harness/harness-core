package io.harness.states;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo.CALLBACK_IDS;
import static io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo.LOG_KEYS;
import static io.harness.beans.sweepingoutputs.ContainerPortDetails.PORT_DETAILS;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_PORT;
import static io.harness.common.CIExecutionConstants.TMP_PATH;
import static io.harness.states.LiteEngineTaskStep.LE_STATUS_TASK_TYPE;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.outcomes.LiteEnginePodDetailsOutcome;
import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CiStepOutcome;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.RunTestsStepInfo;
import io.harness.beans.sweepingoutputs.ContainerPortDetails;
import io.harness.beans.sweepingoutputs.StepLogKeyDetails;
import io.harness.beans.sweepingoutputs.StepTaskDetails;
import io.harness.ci.serializer.PluginCompatibleStepSerializer;
import io.harness.ci.serializer.PluginStepProtobufSerializer;
import io.harness.ci.serializer.RunStepProtobufSerializer;
import io.harness.ci.serializer.RunTestsStepProtobufSerializer;
import io.harness.data.structure.CollectionUtils;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.ci.CIK8ExecuteStepTaskParams;
import io.harness.delegate.task.HDelegateTask;
import io.harness.delegate.task.stepstatus.StepExecutionStatus;
import io.harness.delegate.task.stepstatus.StepMapOutput;
import io.harness.delegate.task.stepstatus.StepStatus;
import io.harness.delegate.task.stepstatus.StepStatusTaskParameters;
import io.harness.delegate.task.stepstatus.StepStatusTaskResponseData;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.logstreaming.LogStreamingHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.product.ci.engine.proto.ExecuteStepRequest;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.steps.StepUtils;
import io.harness.tasks.ResponseData;
import io.harness.yaml.core.timeout.Timeout;

import com.google.inject.Inject;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CI)
public abstract class AbstractStepExecutable implements AsyncExecutable<CIStepInfo> {
  public static final String CI_EXECUTE_STEP = "CI_EXECUTE_STEP";

  @Inject private RunStepProtobufSerializer runStepProtobufSerializer;
  @Inject private PluginStepProtobufSerializer pluginStepProtobufSerializer;
  @Inject private RunTestsStepProtobufSerializer runTestsStepProtobufSerializer;
  @Inject private PluginCompatibleStepSerializer pluginCompatibleStepSerializer;

  @Inject private OutcomeService outcomeService;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private CIDelegateTaskExecutor ciDelegateTaskExecutor;

  @Override
  public Class<CIStepInfo> getStepParametersClass() {
    return CIStepInfo.class;
  }

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, CIStepInfo stepParameters, StepInputPackage inputPackage) {
    StepTaskDetails stepTaskDetails = (StepTaskDetails) executionSweepingOutputResolver.resolve(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(CALLBACK_IDS));
    StepLogKeyDetails stepLogKeyDetails = (StepLogKeyDetails) executionSweepingOutputResolver.resolve(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(LOG_KEYS));
    String stepIdentifier = AmbianceUtils.obtainStepIdentifier(ambiance);
    log.info("Waiting on response for task id {} and step Id {}", stepTaskDetails.getTaskIds().get(stepIdentifier),
        stepIdentifier);
    return AsyncExecutableResponse.newBuilder()
        .addCallbackIds(stepTaskDetails.getTaskIds().get(stepIdentifier))
        .addAllLogKeys(CollectionUtils.emptyIfNull(stepLogKeyDetails.getLogKeys().get(stepIdentifier)))
        .build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, CIStepInfo stepParameters, Map<String, ResponseData> responseDataMap) {
    String stepIdentifier = AmbianceUtils.obtainStepIdentifier(ambiance);
    StepStatusTaskResponseData stepStatusTaskResponseData =
        (StepStatusTaskResponseData) responseDataMap.values().iterator().next();
    StepStatus stepStatus = stepStatusTaskResponseData.getStepStatus();

    log.info("Received response {} for step {}", stepStatus.getStepExecutionStatus(), stepIdentifier);
    if (stepStatus.getStepExecutionStatus() == StepExecutionStatus.SUCCESS) {
      StepResponse.StepOutcome stepOutcome = null;
      if (stepStatus.getOutput() != null) {
        stepOutcome =
            StepResponse.StepOutcome.builder()
                .outcome(
                    CiStepOutcome.builder().outputVariables(((StepMapOutput) stepStatus.getOutput()).getMap()).build())
                .name("output")
                .build();
      }
      return StepResponse.builder().status(Status.SUCCEEDED).stepOutcome(stepOutcome).build();
    } else if (stepStatus.getStepExecutionStatus() == StepExecutionStatus.SKIPPED) {
      return StepResponse.builder().status(Status.SKIPPED).build();
    } else if (stepStatus.getStepExecutionStatus() == StepExecutionStatus.ABORTED) {
      return StepResponse.builder().status(Status.ABORTED).build();
    } else {
      return StepResponse.builder()
          .status(Status.FAILED)
          .failureInfo(FailureInfo.newBuilder()
                           .setErrorMessage(stepStatus.getError())
                           .addAllFailureTypes(EnumSet.of(FailureType.APPLICATION_FAILURE))
                           .build())
          .build();
    }
  }

  @Override
  public void handleAbort(Ambiance ambiance, CIStepInfo stepParameters, AsyncExecutableResponse executableResponse) {}

  // TODO Set timeout and name
  private UnitStep serialiseStep(
      CIStepInfo ciStepInfo, String taskId, String logKey, String stepIdentifier, Integer port, String accountId) {
    switch (ciStepInfo.getNonYamlInfo().getStepInfoType()) {
      case RUN:
        return runStepProtobufSerializer.serializeStepWithStepParameters((RunStepInfo) ciStepInfo, port, taskId, logKey,
            stepIdentifier, ParameterField.createValueField(Timeout.fromString("3600s")), accountId, "run");
      case PLUGIN:
        return pluginStepProtobufSerializer.serializeStepWithStepParameters((PluginStepInfo) ciStepInfo, port, taskId,
            logKey, stepIdentifier, ParameterField.createValueField(Timeout.fromString("3600s")), accountId, "plugin");
      case GCR:
      case DOCKER:
      case ECR:
      case UPLOAD_ARTIFACTORY:
      case UPLOAD_GCS:
      case UPLOAD_S3:
      case SAVE_CACHE_GCS:
      case RESTORE_CACHE_GCS:
      case SAVE_CACHE_S3:
      case RESTORE_CACHE_S3:
        return pluginCompatibleStepSerializer.serializeStepWithStepParameters((PluginCompatibleStep) ciStepInfo, port,
            taskId, logKey, stepIdentifier, ParameterField.createValueField(Timeout.fromString("3600s")), accountId,
            "pluginCompatible");
      case RUN_TESTS:
        return runTestsStepProtobufSerializer.serializeStepWithStepParameters((RunTestsStepInfo) ciStepInfo, port,
            taskId, logKey, stepIdentifier, ParameterField.createValueField(Timeout.fromString("3600s")), accountId,
            "runTest");
      case CLEANUP:
      case TEST:
      case BUILD:
      case SETUP_ENV:
      case GIT_CLONE:
      case LITE_ENGINE_TASK:
      default:
        log.info("serialisation is not implemented");
        return null;
    }
  }

  private String queueDelegateTask(Ambiance ambiance, long timeout, String accountId, CIDelegateTaskExecutor executor,
      UnitStep unitStep, String executionId) {
    LiteEnginePodDetailsOutcome liteEnginePodDetailsOutcome = (LiteEnginePodDetailsOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(LiteEnginePodDetailsOutcome.POD_DETAILS_OUTCOME));
    String ip = liteEnginePodDetailsOutcome.getIpAddress();

    ExecuteStepRequest executeStepRequest =
        ExecuteStepRequest.newBuilder().setExecutionId(executionId).setStep(unitStep).setTmpFilePath(TMP_PATH).build();
    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .parked(false)
                                  .taskType(CI_EXECUTE_STEP)
                                  .parameters(new Object[] {CIK8ExecuteStepTaskParams.builder()
                                                                .ip(ip)
                                                                .port(LITE_ENGINE_PORT)
                                                                .serializedStep(executeStepRequest.toByteArray())
                                                                .build()})
                                  .timeout(timeout)
                                  .build();

    HDelegateTask task =
        (HDelegateTask) StepUtils.prepareDelegateTaskInput(accountId, taskData, ambiance.getSetupAbstractionsMap());

    return executor.queueTask(ambiance.getSetupAbstractionsMap(), task);
  }

  private String queueParkedDelegateTask(
      Ambiance ambiance, long timeout, String accountId, CIDelegateTaskExecutor executor) {
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

  private String getLogKey(Ambiance ambiance) {
    LinkedHashMap<String, String> logAbstractions = StepUtils.generateLogAbstractions(ambiance);
    return LogStreamingHelper.generateLogBaseKey(logAbstractions);
  }

  private Integer getPort(Ambiance ambiance, String stepIdentifier) {
    // Ports are assigned in lite engine step
    ContainerPortDetails containerPortDetails = (ContainerPortDetails) executionSweepingOutputResolver.resolve(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(PORT_DETAILS));

    List<Integer> ports = containerPortDetails.getPortDetails().get(stepIdentifier);

    if (ports.size() != 1) {
      throw new CIStageExecutionException(format("Step [%s] should map to single port", stepIdentifier));
    }

    return ports.get(0);
  }

  private StepStatusTaskResponseData filterStepResponse(Map<String, ResponseData> responseDataMap) {
    // Filter final response from step
    return responseDataMap.entrySet()
        .stream()
        .filter(entry -> entry.getValue() instanceof StepStatusTaskResponseData)
        .findFirst()
        .map(obj -> (StepStatusTaskResponseData) obj.getValue())
        .orElse(null);
  }
}
