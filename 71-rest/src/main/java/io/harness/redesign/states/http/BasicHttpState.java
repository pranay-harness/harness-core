package io.harness.redesign.states.http;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.state.State;
import io.harness.state.StateType;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateResponse.FailureInfo;
import io.harness.state.io.StateResponse.StateResponseBuilder;
import io.harness.state.io.StateTransput;
import software.wings.api.HttpStateExecutionData;
import software.wings.beans.TaskType;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.states.HttpState.HttpStateExecutionResponse;

import java.util.List;
import java.util.Map;

@Redesign
@ExcludeRedesign
@Produces(State.class)
public class BasicHttpState implements State, AsyncExecutable {
  public static final String STATE_TYPE = "BASIC_HTTP";

  @Inject private DelegateService delegateService;

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, StateParameters parameters, List<StateTransput> inputs) {
    BasicHttpStateParameters stateParameters = (BasicHttpStateParameters) parameters;
    HttpTaskParameters httpTaskParameters = HttpTaskParameters.builder()
                                                .url(stateParameters.getUrl())
                                                .body(stateParameters.getBody())
                                                .header(stateParameters.getHeader())
                                                .method(stateParameters.getMethod())
                                                .socketTimeoutMillis(stateParameters.getSocketTimeoutMillis())
                                                .build();

    String waitId = generateUuid();
    DelegateTask delegateTask = DelegateTask.builder()
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
    delegateService.queueTask(delegateTask);
    return AsyncExecutableResponse.builder().callbackId(waitId).build();
  }

  @Override
  public StateResponse handleAsyncResponse(
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
    return StateType.builder().type(STATE_TYPE).build();
  }
}