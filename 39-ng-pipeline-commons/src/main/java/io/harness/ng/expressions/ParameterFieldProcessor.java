package io.harness.ng.expressions;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.ParameterField;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.field.OrchestrationFieldProcessor;
import io.harness.expression.field.ProcessorResult;

public class ParameterFieldProcessor implements OrchestrationFieldProcessor<ParameterField<?>> {
  private final EngineExpressionService engineExpressionService;

  @Inject
  public ParameterFieldProcessor(EngineExpressionService engineExpressionService) {
    this.engineExpressionService = engineExpressionService;
  }

  @Override
  public ProcessorResult process(Ambiance ambiance, ParameterField<?> field) {
    Object newValue;
    boolean updated = true;
    if (field.isExpression()) {
      if (field.isResponseFieldString()) {
        newValue = engineExpressionService.renderExpression(ambiance, field.getExpressionValue());
      } else {
        newValue = engineExpressionService.evaluateExpression(ambiance, field.getExpressionValue());
      }

      if (newValue instanceof String && EngineExpressionEvaluator.hasVariables((String) newValue)) {
        String newExpression = (String) newValue;
        if (newExpression.equals(field.getExpressionValue())) {
          return ProcessorResult.builder().status(ProcessorResult.Status.UNCHANGED).build();
        }

        field.updateWithExpression(newExpression);
        return ProcessorResult.builder().status(ProcessorResult.Status.CHANGED).build();
      }

      field.updateWithValue(newValue);
    } else {
      updated = false;
      newValue = field.getValue();
    }

    if (newValue != null) {
      Object finalValue = engineExpressionService.resolve(ambiance, newValue);
      if (finalValue != null) {
        field.updateWithValue(finalValue);
        updated = true;
      }
    }

    return ProcessorResult.builder()
        .status(updated ? ProcessorResult.Status.CHANGED : ProcessorResult.Status.UNCHANGED)
        .build();
  }
}
