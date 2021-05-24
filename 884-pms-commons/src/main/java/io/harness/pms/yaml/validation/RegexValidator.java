package io.harness.pms.yaml.validation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.EngineExpressionEvaluator;

import java.util.regex.Pattern;

/**
 * This validator handles currentValue to match the given regex.
 * Examples -
 * ${input}.regex(^prod*) #render and use matcher
 * ${input}.regex(^${env.name}_[a-z]+) #render and use matcher
 */
@OwnedBy(HarnessTeam.PIPELINE)
public class RegexValidator implements RuntimeValidator {
  private final EngineExpressionEvaluator engineExpressionEvaluator;
  private final boolean skipUnresolvedExpressionsCheck;

  public RegexValidator(EngineExpressionEvaluator engineExpressionEvaluator, boolean skipUnresolvedExpressionsCheck) {
    this.engineExpressionEvaluator = engineExpressionEvaluator;
    this.skipUnresolvedExpressionsCheck = skipUnresolvedExpressionsCheck;
  }

  @Override
  public RuntimeValidatorResponse isValidValue(Object currentValue, String parameters) {
    if (currentValue == null) {
      return RuntimeValidatorResponse.builder().errorMessage("Current value is null").build();
    }

    String regex = engineExpressionEvaluator.renderExpression(parameters, skipUnresolvedExpressionsCheck);
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
