package io.harness.pms.yaml.validation;

import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expression.PmsEngineExpressionService;

import java.util.regex.Pattern;

/**
 * This validator handles currentValue to match the given regex.
 * Examples -
 * ${input}.regex(^prod*) #render and use matcher
 * ${input}.regex(^${env.name}_[a-z]+) #render and use matcher
 */
public class RegexValidator implements RuntimeValidator {
  private final PmsEngineExpressionService pmsEngineExpressionService;
  private final Ambiance ambiance;

  public RegexValidator(PmsEngineExpressionService pmsEngineExpressionService, Ambiance ambiance) {
    this.pmsEngineExpressionService = pmsEngineExpressionService;
    this.ambiance = ambiance;
  }

  @Override
  public RuntimeValidatorResponse isValidValue(Object currentValue, String parameters) {
    if (currentValue == null) {
      return RuntimeValidatorResponse.builder().errorMessage("Current value is null").build();
    }

    String regex = pmsEngineExpressionService.renderExpression(ambiance, parameters);
    if (currentValue instanceof String) {
      if (!ExpressionUtils.matchesPattern(Pattern.compile(regex), (String) currentValue)) {
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
