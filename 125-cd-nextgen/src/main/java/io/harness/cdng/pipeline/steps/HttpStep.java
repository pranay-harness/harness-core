package io.harness.cdng.pipeline.steps;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.execution.status.Status;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.facilitator.modes.task.TaskExecutable;
import io.harness.ngpipeline.orchestration.StepUtils;
import io.harness.redesign.states.http.BasicHttpStepParameters;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepOutcome;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.tasks.ResponseData;
import io.harness.tasks.Task;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.HttpStateExecutionData;
import software.wings.beans.TaskType;
import software.wings.sm.states.HttpState.HttpStateExecutionResponse;

import java.util.Map;

@OwnedBy(CDC)
@Redesign
@Slf4j
public class HttpStep implements Step, TaskExecutable<BasicHttpStepParameters> {
  public static final StepType STEP_TYPE = StepType.builder().type(ExecutionNodeType.HTTP.getName()).build();
  private static final int socketTimeoutMillis = 10000;

  @Override
  public Task obtainTask(Ambiance ambiance, BasicHttpStepParameters stepParameters, StepInputPackage inputPackage) {
    HttpTaskParameters httpTaskParameters = HttpTaskParameters.builder()
                                                .url(stepParameters.getUrl())
                                                .body(stepParameters.getBody())
                                                .header(stepParameters.getHeader())
                                                .method(stepParameters.getMethod())
                                                .socketTimeoutMillis(socketTimeoutMillis)
                                                .build();

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(TaskData.DEFAULT_ASYNC_CALL_TIMEOUT)
                                  .taskType(TaskType.HTTP.name())
                                  .parameters(new Object[] {httpTaskParameters})
                                  .build();
    return StepUtils.prepareDelegateTaskInput(
        ambiance.getSetupAbstractions().get("accountId"), taskData, ambiance.getSetupAbstractions());
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
          .failureInfo(FailureInfo.builder()
                           .errorMessage(errorNotifyResponseData.getErrorMessage())
                           .failureTypes(errorNotifyResponseData.getFailureTypes())
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
