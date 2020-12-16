package io.harness.redesign.states.http;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.algorithm.HashGenerator;
import io.harness.delegate.Capability;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskLogAbstractions;
import io.harness.delegate.TaskMode;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.delegate.TaskType;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.DelegateTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.Cd1SetupFields;
import io.harness.tasks.ResponseData;

import software.wings.api.HttpStateExecutionData;
import software.wings.sm.states.HttpState.HttpStateExecutionResponse;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;

@OwnedBy(CDC)
@Redesign
@Slf4j
public class BasicHttpStep implements TaskExecutable<BasicHttpStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType("BASIC_HTTP").build();
  private static final int socketTimeoutMillis = 10000;

  @Inject KryoSerializer kryoSerializer;

  @Override
  public Class<BasicHttpStepParameters> getStepParametersClass() {
    return BasicHttpStepParameters.class;
  }

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, BasicHttpStepParameters stepParameters, StepInputPackage inputPackage) {
    HttpTaskParameters httpTaskParameters = HttpTaskParameters.builder()
                                                .url(stepParameters.getUrl())
                                                .body(stepParameters.getBody())
                                                .header(stepParameters.getHeader())
                                                .method(stepParameters.getMethod())
                                                .socketTimeoutMillis(socketTimeoutMillis)
                                                .build();

    List<ExecutionCapability> capabilities =
        ListUtils.emptyIfNull(httpTaskParameters.fetchRequiredExecutionCapabilities(null));

    DelegateTaskRequest.Builder requestBuilder =
        DelegateTaskRequest.newBuilder()
            .setAccountId(ambiance.getSetupAbstractionsMap().get("accountId"))
            .setDetails(
                TaskDetails.newBuilder()
                    .setKryoParameters(ByteString.copyFrom(kryoSerializer.asBytes(httpTaskParameters)))
                    .setExecutionTimeout(Duration.newBuilder().setSeconds(DEFAULT_ASYNC_CALL_TIMEOUT * 1000).build())
                    // TODO : Change this somehow and obtain from ambiance
                    .setExpressionFunctorToken(HashGenerator.generateIntegerHash())
                    .setMode(TaskMode.ASYNC)
                    .setParked(false)
                    .setType(TaskType.newBuilder().setType("HTTP").build())
                    .build())
            .setLogAbstractions(
                TaskLogAbstractions.newBuilder()
                    .putAllValues(ImmutableMap.of("pipelineExecutionId", ambiance.getPlanExecutionId(), "stepId",
                        Objects.requireNonNull(AmbianceUtils.obtainCurrentRuntimeId(ambiance))))
                    .build());
    if (isNotEmpty(capabilities)) {
      requestBuilder.addAllCapabilities(
          capabilities.stream()
              .map(capability
                  -> Capability.newBuilder()
                         .setKryoCapability(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(capability)))
                         .build())
              .collect(toList()));
    }
    TaskSetupAbstractions.Builder abstractionBuilder =
        TaskSetupAbstractions.newBuilder().putValues(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, generateUuid());

    String appId = ambiance.getSetupAbstractionsMap().get("appId");
    if (appId != null) {
      abstractionBuilder.putValues(Cd1SetupFields.APP_ID_FIELD, appId);
    }
    return TaskRequest.newBuilder()
        .setDelegateTaskRequest(requestBuilder.setSetupAbstractions(abstractionBuilder.build()).build())
        .setTaskCategory(TaskCategory.DELEGATE_TASK_V1)
        .build();
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, BasicHttpStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    StepResponseBuilder responseBuilder = StepResponse.builder();
    ResponseData notifyResponseData = responseDataMap.values().iterator().next();
    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) notifyResponseData;
      responseBuilder.status(Status.FAILED);
      responseBuilder
          .failureInfo(FailureInfo.newBuilder()
                           .setErrorMessage(errorNotifyResponseData.getErrorMessage())
                           .addAllFailureTypes(
                               EngineExceptionUtils.transformFailureTypes(errorNotifyResponseData.getFailureTypes()))
                           .build())
          .build();
    } else {
      HttpStateExecutionResponse httpStateExecutionResponse = (HttpStateExecutionResponse) notifyResponseData;
      HttpStateExecutionData executionData = HttpStateExecutionData.builder()
                                                 .httpUrl(stepParameters.getUrl())
                                                 .httpMethod(stepParameters.getMethod())
                                                 .httpResponseCode(httpStateExecutionResponse.getHttpResponseCode())
                                                 .httpResponseBody(httpStateExecutionResponse.getHttpResponseBody())
                                                 .status(httpStateExecutionResponse.getExecutionStatus())
                                                 .errorMsg(httpStateExecutionResponse.getErrorMessage())
                                                 .build();
      // Just Place holder for now till we have assertions
      if (httpStateExecutionResponse.getHttpResponseCode() == 500) {
        responseBuilder.status(Status.FAILED);
      } else {
        responseBuilder.status(Status.SUCCEEDED);
      }
      responseBuilder.stepOutcome(StepOutcome.builder().name("http").outcome(executionData).build());
    }
    return responseBuilder.build();
  }
}
