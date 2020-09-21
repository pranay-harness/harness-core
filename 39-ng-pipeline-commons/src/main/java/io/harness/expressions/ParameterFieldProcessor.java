package io.harness.expressions;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.InputSetValidator;
import io.harness.beans.ParameterField;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.field.OrchestrationFieldProcessor;
import io.harness.expression.field.ProcessorResult;
import io.harness.inputset.validators.InputSetValidatorFactory;
import io.harness.inputset.validators.RuntimeValidator;
import io.harness.inputset.validators.RuntimeValidatorResponse;

public class ParameterFieldProcessor implements OrchestrationFieldProcessor<ParameterField<?>> {
  private final EngineExpressionService engineExpressionService;
  private final InputSetValidatorFactory inputSetValidatorFactory;

  @Inject
  public ParameterFieldProcessor(
      EngineExpressionService engineExpressionService, InputSetValidatorFactory inputSetValidatorFactory) {
    this.engineExpressionService = engineExpressionService;
    this.inputSetValidatorFactory = inputSetValidatorFactory;
  }

  @Override
  public ProcessorResult process(Ambiance ambiance, ParameterField<?> field) {
    Object newValue;
    boolean updated = true;
    InputSetValidator inputSetValidator = field.getInputSetValidator();
    if (field.isExpression()) {
      if (field.isTypeString()) {
        newValue = engineExpressionService.renderExpression(ambiance, field.getExpressionValue());
      } else {
        newValue = engineExpressionService.evaluateExpression(ambiance, field.getExpressionValue());
      }

      if (newValue instanceof String && EngineExpressionEvaluator.hasVariables((String) newValue)) {
        String newExpression = (String) newValue;

        if (field.isTypeString()) {
          field.updateWithValue(newValue);
          return validateUsingValidator(newValue, inputSetValidator, ambiance);
        }

        if (newExpression.equals(field.getExpressionValue())) {
          return ProcessorResult.builder().status(ProcessorResult.Status.UNCHANGED).build();
        }

        field.updateWithExpression(newExpression);
        return validateUsingValidator(newValue, inputSetValidator, ambiance);
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
        ProcessorResult processorResult = validateUsingValidator(newValue, inputSetValidator, ambiance);
        if (processorResult.getStatus() == ProcessorResult.Status.ERROR) {
          return processorResult;
        }
        updated = true;
      }
    }

    return ProcessorResult.builder()
        .status(updated ? ProcessorResult.Status.CHANGED : ProcessorResult.Status.UNCHANGED)
        .build();
  }

  private ProcessorResult validateUsingValidator(Object value, InputSetValidator inputSetValidator, Ambiance ambiance) {
    if (inputSetValidator != null) {
      RuntimeValidator runtimeValidator =
          inputSetValidatorFactory.obtainValidator(inputSetValidator, engineExpressionService, ambiance);
      RuntimeValidatorResponse validatorResponse =
          runtimeValidator.isValidValue(value, inputSetValidator.getParameters());
      if (!validatorResponse.isValid()) {
        return ProcessorResult.builder()
            .status(ProcessorResult.Status.ERROR)
            .message(validatorResponse.getErrorMessage())
            .build();
      }
    }
    return ProcessorResult.builder().status(ProcessorResult.Status.CHANGED).build();
  }
}
