package io.harness.pms.yaml;

import io.harness.expression.EngineExpressionEvaluator;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.expression.ProcessorResult;
import io.harness.pms.yaml.validation.InputSetValidator;
import io.harness.pms.yaml.validation.InputSetValidatorFactory;
import io.harness.pms.yaml.validation.RuntimeValidator;
import io.harness.pms.yaml.validation.RuntimeValidatorResponse;

import com.google.inject.Inject;
import org.bson.Document;

public class ParameterFieldProcessor {
  private final EngineExpressionService engineExpressionService;
  private final InputSetValidatorFactory inputSetValidatorFactory;

  @Inject
  public ParameterFieldProcessor(
      EngineExpressionService engineExpressionService, InputSetValidatorFactory inputSetValidatorFactory) {
    this.engineExpressionService = engineExpressionService;
    this.inputSetValidatorFactory = inputSetValidatorFactory;
  }

  public ProcessorResult process(Ambiance ambiance, ParameterDocumentField field) {
    if (field == null) {
      return ProcessorResult.builder().build();
    }

    Object newValue = null;
    InputSetValidator inputSetValidator = field.getInputSetValidator();
    if (field.isExpression()) {
      if (field.isTypeString()) {
        newValue = engineExpressionService.renderExpression(ambiance, field.getExpressionValue());
      } else {
        newValue = engineExpressionService.evaluateExpression(ambiance, field.getExpressionValue());
      }

      if (newValue instanceof String && EngineExpressionEvaluator.hasVariables((String) newValue)) {
        String newExpression = (String) newValue;
        if (newExpression.equals(field.getExpressionValue())) {
          return ProcessorResult.builder().build();
        }

        field.updateWithExpression(newExpression);
        return validateUsingValidator(newValue, inputSetValidator, ambiance);
      }

      field.updateWithValue(newValue);
    }

    if (field.getValueDoc() == null) {
      return ProcessorResult.builder().build();
    }

    Document doc = field.getValueDoc();
    Object valueField = doc.get(ParameterFieldValueWrapper.VALUE_FIELD);
    if (valueField != null) {
      Object finalValue = engineExpressionService.resolve(ambiance, valueField);
      if (finalValue != null) {
        field.updateWithValue(finalValue);
        ProcessorResult processorResult = validateUsingValidator(finalValue, inputSetValidator, ambiance);
        if (processorResult.isError()) {
          return processorResult;
        }
      }
    }

    return ProcessorResult.builder().build();
  }

  private ProcessorResult validateUsingValidator(Object value, InputSetValidator inputSetValidator, Ambiance ambiance) {
    if (inputSetValidator != null) {
      RuntimeValidator runtimeValidator =
          inputSetValidatorFactory.obtainValidator(inputSetValidator, engineExpressionService, ambiance);
      RuntimeValidatorResponse validatorResponse =
          runtimeValidator.isValidValue(value, inputSetValidator.getParameters());
      if (!validatorResponse.isValid()) {
        return ProcessorResult.builder().error(true).message(validatorResponse.getErrorMessage()).build();
      }
    }
    return ProcessorResult.builder().build();
  }
}
