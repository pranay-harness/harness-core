package io.harness.redesign.states.http;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.modes.task.AsyncTaskExecutable;
import io.harness.state.StateType;
import io.harness.state.Step;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateResponse.FailureInfo;
import io.harness.state.io.StateResponse.StateResponseBuilder;
import io.harness.state.io.StateTransput;
import software.wings.api.HttpStateExecutionData;
import software.wings.beans.TaskType;
import software.wings.sm.states.HttpState.HttpStateExecutionResponse;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Redesign
@ExcludeRedesign
@Produces(Step.class)
public class BasicHttpStep implements Step, AsyncTaskExecutable {
  public static final StateType STATE_TYPE = StateType.builder().type("BASIC_HTTP").build();

  @Override
  public DelegateTask obtainTask(Ambiance ambiance, StateParameters parameters, List<StateTransput> inputs) {
    BasicHttpStateParameters stateParameters = (BasicHttpStateParameters) parameters;
    HttpTaskParameters httpTaskParameters = HttpTaskParameters.builder()
                                                .url(stateParameters.getUrl())
                                                .body(stateParameters.getBody())
                                                .header(stateParameters.getHeader())
                                                .method(stateParameters.getMethod())
                                                .socketTimeoutMillis(stateParameters.getSocketTimeoutMillis())
                                                .build();

    String waitId = generateUuid();
    return DelegateTask.builder()
        .accountId(ambiance.getSetupAbstractions().get("accountId"))
        .waitId(waitId)
        .appId(ambiance.getSetupAbstractions().get("appId"))
        .data(TaskData.builder()
                  .taskType(TaskType.HTTP.name())
                  .parameters(new Object[] {httpTaskParameters})
                  .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                  .build())
        .infrastructureMappingId(waitId)
        .build();
  }

  @Override
  public StateResponse handleTaskResult(
      Ambiance ambiance, StateParameters parameters, Map<String, ResponseData> responseDataMap) {
    BasicHttpStateParameters stateParameters = (BasicHttpStateParameters) parameters;
    StateResponseBuilder responseBuilder = StateResponse.builder();
    ResponseData notifyResponseData = responseDataMap.values().iterator().next();
    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) notifyResponseData;
      responseBuilder.status(NodeExecutionStatus.FAILED);
      responseBuilder
          .failureInfo(FailureInfo.builder()
                           .errorMessage(errorNotifyResponseData.getErrorMessage())
                           .failureTypes(errorNotifyResponseData.getFailureTypes())
                           .build())
          .build();
    } else {
      HttpStateExecutionResponse httpStateExecutionResponse = (HttpStateExecutionResponse) notifyResponseData;
      HttpStateExecutionData executionData = HttpStateExecutionData.builder()
                                                 .httpUrl(stateParameters.getUrl())
                                                 .httpMethod(stateParameters.getMethod())
                                                 .httpResponseCode(httpStateExecutionResponse.getHttpResponseCode())
                                                 .httpResponseBody(httpStateExecutionResponse.getHttpResponseBody())
                                                 .status(httpStateExecutionResponse.getExecutionStatus())
                                                 .errorMsg(httpStateExecutionResponse.getErrorMessage())
                                                 .build();
      responseBuilder.status(NodeExecutionStatus.SUCCEEDED);
      responseBuilder.outcome("http", executionData);
    }
    return responseBuilder.build();
  }

  @Override
  public StateType getType() {
    return STATE_TYPE;
  }
}