package io.harness.expression.field.dummy;

import io.harness.ambiance.Ambiance;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.field.OrchestrationFieldProcessor;
import io.harness.expression.field.ProcessorResult;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class DummyOrchestrationFieldProcessor implements OrchestrationFieldProcessor<DummyOrchestrationField> {
  private EngineExpressionEvaluator expressionEvaluator;

  @Override
  public ProcessorResult process(Ambiance ambiance, DummyOrchestrationField field) {
    Object newValue;
    boolean updated = true;
    if (field.isExpression()) {
      if (field.isString()) {
        newValue = expressionEvaluator.renderExpression(field.getExpressionValue());
      } else {
        newValue = expressionEvaluator.evaluateExpression(field.getExpressionValue());
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
      Object finalValue = expressionEvaluator.resolve(newValue);
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
