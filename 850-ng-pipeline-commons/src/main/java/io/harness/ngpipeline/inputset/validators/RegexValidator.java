package io.harness.ngpipeline.inputset.validators;

import io.harness.common.NGExpressionUtils;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.pms.contracts.ambiance.Ambiance;

import java.util.regex.Pattern;

/**
 * This validator handles currentValue to match the given regex.
 * Examples -
 * ${input}.regex(^prod*) #render and use matcher
 * ${input}.regex(^${env.name}_[a-z]+) #render and use matcher
 */
public class RegexValidator implements RuntimeValidator {
  private final EngineExpressionService engineExpressionService;
  private final Ambiance ambiance;

  public RegexValidator(EngineExpressionService engineExpressionService, Ambiance ambiance) {
    this.engineExpressionService = engineExpressionService;
    this.ambiance = ambiance;
  }

  @Override
  public RuntimeValidatorResponse isValidValue(Object currentValue, String parameters) {
    if (currentValue == null) {
      return RuntimeValidatorResponse.builder().errorMessage("Current value is null").build();
    }

    String regex = engineExpressionService.renderExpression(ambiance, parameters);
    if (currentValue instanceof String) {
      if (!NGExpressionUtils.matchesPattern(Pattern.compile(regex), (String) currentValue)) {
        return RuntimeValidatorResponse.builder().errorMessage("Current value does not match with given regex").build();
      }
      return RuntimeValidatorResponse.builder().isValid(true).build();
    } else {
      return RuntimeValidatorResponse.builder()
          .errorMessage("Regex do not handle value of type: " + currentValue.getClass())
          .build();
    }
  }
}
