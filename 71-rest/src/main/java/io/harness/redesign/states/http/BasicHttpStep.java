package io.harness.redesign.states.http;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.task.TaskExecutable;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepOutcome;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.state.io.StepTransput;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.HttpStateExecutionData;
import software.wings.beans.TaskType;
import software.wings.sm.states.HttpState.HttpStateExecutionResponse;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Redesign
@Slf4j
public class BasicHttpStep implements Step, TaskExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("BASIC_HTTP").build();
  private static final int socketTimeoutMillis = 10000;

  @Override
  public DelegateTask obtainTask(Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs) {
    BasicHttpStepParameters parameters = (BasicHttpStepParameters) stepParameters;
    HttpTaskParameters httpTaskParameters = HttpTaskParameters.builder()
                                                .url(parameters.getUrl())
                                                .body(parameters.getBody())
                                                .header(parameters.getHeader())
                                                .method(parameters.getMethod())
                                                .socketTimeoutMillis(socketTimeoutMillis)
                                                .build();

    String waitId = generateUuid();
    return DelegateTask.builder()
        .accountId((String) ambiance.getInputArgs().get("accountId"))
        .waitId(waitId)
        .appId((String) ambiance.getInputArgs().get("appId"))
        .data(TaskData.builder()
                  .taskType(TaskType.HTTP.name())
                  .parameters(new Object[] {httpTaskParameters})
                  .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                  .build())
        .infrastructureMappingId(waitId)
        .build();
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    BasicHttpStepParameters parameters = (BasicHttpStepParameters) stepParameters;
    StepResponseBuilder responseBuilder = StepResponse.builder();
    ResponseData notifyResponseData = responseDataMap.values().iterator().next();
    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) notifyResponseData;
      responseBuilder.status(Status.FAILED);
      responseBuilder
          .failureInfo(FailureInfo.builder()
                           .errorMessage(errorNotifyResponseData.getErrorMessage())
                           .failureTypes(errorNotifyResponseData.getFailureTypes())
                           .build())
          .build();
    } else {
      HttpStateExecutionResponse httpStateExecutionResponse = (HttpStateExecutionResponse) notifyResponseData;
      HttpStateExecutionData executionData = HttpStateExecutionData.builder()
                                                 .httpUrl(parameters.getUrl())
                                                 .httpMethod(parameters.getMethod())
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