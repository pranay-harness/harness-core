package io.harness.steps.http;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.NGTimeConversionHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpStepResponse;
import io.harness.delegate.task.http.HttpTaskParametersNg;
import io.harness.delegate.task.http.HttpTaskParametersNg.HttpTaskParametersNgBuilder;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.http.HttpHeaderConfig;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;

import software.wings.beans.TaskType;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class HttpStep implements TaskExecutable<StepElementParameters, HttpStepResponse> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(StepSpecTypeConstants.HTTP).build();

  @Inject private KryoSerializer kryoSerializer;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    int socketTimeoutMillis = (int) NGTimeConversionHelper.convertTimeStringToMilliseconds("10m");
    if (stepParameters.getTimeout() != null && stepParameters.getTimeout().getValue() != null) {
      socketTimeoutMillis =
          (int) NGTimeConversionHelper.convertTimeStringToMilliseconds(stepParameters.getTimeout().getValue());
    }
    HttpStepParameters httpStepParameters = (HttpStepParameters) stepParameters.getSpec();
    HttpTaskParametersNgBuilder httpTaskParametersNgBuilder = HttpTaskParametersNg.builder()
                                                                  .url(httpStepParameters.getUrl().getValue())
                                                                  .method(httpStepParameters.getMethod().getValue())
                                                                  .socketTimeoutMillis(socketTimeoutMillis);

    if (EmptyPredicate.isNotEmpty(httpStepParameters.getHeaders())) {
      List<HttpHeaderConfig> headers = new ArrayList<>();
      httpStepParameters.getHeaders().keySet().forEach(key
          -> headers.add(HttpHeaderConfig.builder().key(key).value(httpStepParameters.getHeaders().get(key)).build()));
      httpTaskParametersNgBuilder.requestHeader(headers);
    }

    if (httpStepParameters.getRequestBody() != null) {
      httpTaskParametersNgBuilder.body(httpStepParameters.getRequestBody().getValue());
    }

    final TaskData taskData =
        TaskData.builder()
            .async(true)
            .timeout(NGTimeConversionHelper.convertTimeStringToMilliseconds(stepParameters.getTimeout().getValue()))
            .taskType(TaskType.HTTP_TASK_NG.name())
            .parameters(new Object[] {httpTaskParametersNgBuilder.build()})
            .build();
    return StepUtils.prepareTaskRequestWithTaskSelector(ambiance, taskData, kryoSerializer,
        TaskSelectorYaml.toTaskSelector(httpStepParameters.delegateSelectors.getValue()));
  }

  @Override
  public StepResponse handleTaskResult(Ambiance ambiance, StepElementParameters stepParameters,
      ThrowingSupplier<HttpStepResponse> responseSupplier) throws Exception {
    StepResponseBuilder responseBuilder = StepResponse.builder();
    HttpStepResponse httpStepResponse = responseSupplier.get();

    HttpStepParameters httpStepParameters = (HttpStepParameters) stepParameters.getSpec();
    Map<String, Object> outputVariables = httpStepParameters.getOutputVariables();
    Map<String, String> outputVariablesEvaluated = evaluateOutputVariables(outputVariables, httpStepResponse);

    boolean assertionSuccessful = validateAssertions(httpStepResponse, httpStepParameters);

    HttpOutcome executionData = HttpOutcome.builder()
                                    .httpUrl(httpStepParameters.getUrl().getValue())
                                    .httpMethod(httpStepParameters.getMethod().getValue())
                                    .httpResponseCode(httpStepResponse.getHttpResponseCode())
                                    .httpResponseBody(httpStepResponse.getHttpResponseBody())
                                    .status(httpStepResponse.getCommandExecutionStatus())
                                    .errorMsg(httpStepResponse.getErrorMessage())
                                    .outputVariables(outputVariablesEvaluated)
                                    .build();

    // Just Place holder for now till we have assertions
    if (httpStepResponse.getHttpResponseCode() == 500 || !assertionSuccessful) {
      responseBuilder.status(Status.FAILED);
      if (!assertionSuccessful) {
        responseBuilder.failureInfo(FailureInfo.newBuilder().setErrorMessage("assertion failed").build());
      }
    } else {
      responseBuilder.status(Status.SUCCEEDED);
    }
    responseBuilder.stepOutcome(
        StepOutcome.builder().name(YAMLFieldNameConstants.OUTPUT).outcome(executionData).build());
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
