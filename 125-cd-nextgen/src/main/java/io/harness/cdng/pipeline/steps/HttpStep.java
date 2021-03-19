package io.harness.cdng.pipeline.steps;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.expressions.HttpExpressionEvaluator;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.NGTaskType;
import io.harness.common.NGTimeConversionHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpStepResponse;
import io.harness.delegate.task.http.HttpTaskParametersNg;
import io.harness.delegate.task.http.HttpTaskParametersNg.HttpTaskParametersNgBuilder;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.expression.EngineExpressionEvaluator;
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
import io.harness.pms.sdk.core.steps.io.RollbackOutcome;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.yaml.ParameterField;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepUtils;
import io.harness.tasks.ResponseData;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class HttpStep implements TaskExecutable<HttpStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(ExecutionNodeType.HTTP.getYamlType()).build();

  @Inject private KryoSerializer kryoSerializer;

  @Override
  public Class<HttpStepParameters> getStepParametersClass() {
    return HttpStepParameters.class;
  }

  @Override
  public TaskRequest obtainTask(Ambiance ambiance, HttpStepParameters stepParameters, StepInputPackage inputPackage) {
    int socketTimeoutMillis = (int) NGTimeConversionHelper.convertTimeStringToMilliseconds("10m");
    if (stepParameters.getTimeout() != null && stepParameters.getTimeout().getValue() != null) {
      socketTimeoutMillis =
          (int) NGTimeConversionHelper.convertTimeStringToMilliseconds(stepParameters.getTimeout().getValue());
    }
    HttpTaskParametersNgBuilder httpTaskParametersNgBuilder = HttpTaskParametersNg.builder()
                                                                  .url(stepParameters.getUrl().getValue())
                                                                  .method(stepParameters.getMethod().getValue())
                                                                  .socketTimeoutMillis(socketTimeoutMillis);

    if (EmptyPredicate.isNotEmpty(stepParameters.getHeaders())) {
      List<HttpHeaderConfig> headers = new ArrayList<>();
      stepParameters.getHeaders().keySet().forEach(
          key -> headers.add(HttpHeaderConfig.builder().key(key).value(stepParameters.getHeaders().get(key)).build()));
      httpTaskParametersNgBuilder.requestHeader(headers);
    }

    if (stepParameters.getRequestBody() != null) {
      httpTaskParametersNgBuilder.body(stepParameters.getRequestBody().getValue());
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
      responseBuilder.failureInfo(FailureInfo.newBuilder()
                                      .setErrorMessage(errorNotifyResponseData.getErrorMessage())
                                      .addAllFailureTypes(EngineExceptionUtils.transformToOrchestrationFailureTypes(
                                          errorNotifyResponseData.getFailureTypes()))
                                      .build());
      if (stepParameters.getRollbackInfo() != null) {
        responseBuilder.stepOutcome(
            StepOutcome.builder()
                .name("RollbackOutcome")
                .outcome(RollbackOutcome.builder().rollbackInfo(stepParameters.getRollbackInfo()).build())
                .build());
      }
    } else {
      HttpStepResponse httpStepResponse = (HttpStepResponse) notifyResponseData;

      Map<String, Object> outputVariables = stepParameters.getOutputVariables();
      Map<String, String> outputVariablesEvaluated = evaluateOutputVariables(outputVariables, httpStepResponse);

      boolean assertionSuccessful = validateAssertions(httpStepResponse, stepParameters);

      HttpOutcome executionData = HttpOutcome.builder()
                                      .httpUrl(stepParameters.getUrl().getValue())
                                      .httpMethod(stepParameters.getMethod().getValue())
                                      .httpResponseCode(httpStepResponse.getHttpResponseCode())
                                      .httpResponseBody(httpStepResponse.getHttpResponseBody())
                                      .status(httpStepResponse.getCommandExecutionStatus())
                                      .errorMsg(httpStepResponse.getErrorMessage())
                                      .outputVariables(outputVariablesEvaluated)
                                      .build();

      // Just Place holder for now till we have assertions
      if (httpStepResponse.getHttpResponseCode() == 500 || !assertionSuccessful) {
        responseBuilder.status(Status.FAILED);
        if (stepParameters.getRollbackInfo() != null) {
          responseBuilder.stepOutcome(
              StepOutcome.builder()
                  .name("RollbackOutcome")
                  .outcome(RollbackOutcome.builder().rollbackInfo(stepParameters.getRollbackInfo()).build())
                  .build());
        }
        if (!assertionSuccessful) {
          responseBuilder.failureInfo(FailureInfo.newBuilder().setErrorMessage("assertion failed").build());
        }
      } else {
        responseBuilder.status(Status.SUCCEEDED);
      }
      responseBuilder.stepOutcome(
          StepOutcome.builder().name(OutcomeExpressionConstants.OUTPUT).outcome(executionData).build());
    }
    return responseBuilder.build();
  }

  public static boolean validateAssertions(HttpStepResponse httpStepResponse, HttpStepParameters stepParameters) {
    if (ParameterField.isNull(stepParameters.getAssertion())) {
      return true;
    }

    HttpExpressionEvaluator evaluator = new HttpExpressionEvaluator(httpStepResponse.getHttpResponseCode());
    String assertion = (String) stepParameters.getAssertion().fetchFinalValue();
    if (assertion == null || EmptyPredicate.isEmpty(assertion.trim())) {
      return true;
    }

    try {
      Map<String, Object> context = ImmutableMap.<String, Object>builder()
                                        .put("httpResponseBody", httpStepResponse.getHttpResponseBody())
                                        .build();
      Object value = evaluator.evaluateExpression(assertion, context);
      if (!(value instanceof Boolean)) {
        throw new InvalidRequestException(String.format(
            "Expected boolean assertion, got %s value", value == null ? "null" : value.getClass().getSimpleName()));
      }
      return (boolean) value;
    } catch (Exception e) {
      throw new InvalidRequestException("Assertion provided is not a valid expression", e);
    }
  }

  public static Map<String, String> evaluateOutputVariables(
      Map<String, Object> outputVariables, HttpStepResponse httpStepResponse) {
    Map<String, String> outputVariablesEvaluated = new LinkedHashMap<>();
    if (outputVariables != null) {
      Map<String, Object> context = ImmutableMap.<String, Object>builder()
                                        .put("httpResponseBody", httpStepResponse.getHttpResponseBody())
                                        .build();
      EngineExpressionEvaluator expressionEvaluator = new EngineExpressionEvaluator(null);
      outputVariables.keySet().forEach(name -> {
        Object expression = outputVariables.get(name);
        if (expression instanceof ParameterField) {
          ParameterField<?> expr = (ParameterField<?>) expression;
          if (expr.isExpression()) {
            Object evaluatedValue = expressionEvaluator.evaluateExpression(expr.getExpressionValue(), context);
            if (evaluatedValue != null) {
              outputVariablesEvaluated.put(name, evaluatedValue.toString());
            }
          }
        }
      });
    }
    return outputVariablesEvaluated;
  }
}
