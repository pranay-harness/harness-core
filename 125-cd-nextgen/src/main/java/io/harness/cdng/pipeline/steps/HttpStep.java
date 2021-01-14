package io.harness.cdng.pipeline.steps;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.NGTaskType;
import io.harness.common.NGTimeConversionHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpStepResponse;
import io.harness.delegate.task.http.HttpTaskParametersNg;
import io.harness.delegate.task.http.HttpTaskParametersNg.HttpTaskParametersNgBuilder;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.http.HttpHeaderConfig;
import io.harness.http.HttpOutcome;
import io.harness.http.HttpStepParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepUtils;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class HttpStep implements TaskExecutable<HttpStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(ExecutionNodeType.HTTP.getYamlType()).build();
  private static final int socketTimeoutMillis = 10000;

  @Inject private KryoSerializer kryoSerializer;

  @Override
  public Class<HttpStepParameters> getStepParametersClass() {
    return HttpStepParameters.class;
  }

  @Override
  public TaskRequest obtainTask(Ambiance ambiance, HttpStepParameters stepParameters, StepInputPackage inputPackage) {
    HttpTaskParametersNgBuilder httpTaskParametersNgBuilder = HttpTaskParametersNg.builder()
                                                                  .url(stepParameters.getUrl().getValue())
                                                                  .body(stepParameters.getRequestBody().getValue())
                                                                  .method(stepParameters.getMethod().getValue())
                                                                  .socketTimeoutMillis(socketTimeoutMillis);

    if (EmptyPredicate.isNotEmpty(stepParameters.getHeaders())) {
      httpTaskParametersNgBuilder.requestHeader(stepParameters.getHeaders().stream().collect(
          Collectors.toMap(HttpHeaderConfig::getKey, HttpHeaderConfig::getValue)));
    }

    final TaskData taskData =
        TaskData.builder()
            .async(true)
            .timeout(NGTimeConversionHelper.convertTimeStringToMilliseconds(stepParameters.getTimeout().getValue()))
            .taskType(NGTaskType.HTTP_TASK_NG.name())
            .parameters(new Object[] {httpTaskParametersNgBuilder.build()})
            .build();
    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer);
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, HttpStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    StepResponseBuilder responseBuilder = StepResponse.builder();
    ResponseData notifyResponseData = responseDataMap.values().iterator().next();
    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) notifyResponseData;
      responseBuilder.status(Status.FAILED);
      responseBuilder
          .failureInfo(FailureInfo.newBuilder()
                           .setErrorMessage(errorNotifyResponseData.getErrorMessage())
                           .addAllFailureTypes(EngineExceptionUtils.transformToOrchestrationFailureTypes(
                               errorNotifyResponseData.getFailureTypes()))
                           .build())
          .build();
    } else {
      HttpStepResponse httpStepResponse = (HttpStepResponse) notifyResponseData;

      HttpOutcome executionData = HttpOutcome.builder()
                                      .httpUrl(stepParameters.getUrl().getValue())
                                      .httpMethod(stepParameters.getMethod().getValue())
                                      .httpResponseCode(httpStepResponse.getHttpResponseCode())
                                      .httpResponseBody(httpStepResponse.getHttpResponseBody())
                                      .status(httpStepResponse.getCommandExecutionStatus())
                                      .errorMsg(httpStepResponse.getErrorMessage())
                                      .build();

      // Just Place holder for now till we have assertions
      if (httpStepResponse.getHttpResponseCode() == 500) {
        responseBuilder.status(Status.FAILED);
      } else {
        responseBuilder.status(Status.SUCCEEDED);
      }
      responseBuilder.stepOutcome(
          StepOutcome.builder().name(OutcomeExpressionConstants.OUTPUT).outcome(executionData).build());
    }
    return responseBuilder.build();
  }
}
