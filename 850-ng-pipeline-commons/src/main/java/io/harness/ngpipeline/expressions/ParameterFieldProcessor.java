package io.harness.ngpipeline.expressions;

import io.harness.engine.expressions.EngineExpressionService;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.ngpipeline.inputset.validators.InputSetValidatorFactory;
import io.harness.ngpipeline.inputset.validators.RuntimeValidator;
import io.harness.ngpipeline.inputset.validators.RuntimeValidatorResponse;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.expression.OrchestrationFieldProcessor;
import io.harness.pms.expression.ProcessorResult;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.validation.InputSetValidator;

import com.google.inject.Inject;

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
