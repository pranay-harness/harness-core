package io.harness.common;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.EngineExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class NGExpressionUtils {
  private static final Pattern InputSetVariablePattern = Pattern.compile(
      EngineExpressionEvaluator.EXPR_START_ESC + "input" + EngineExpressionEvaluator.EXPR_END_ESC + ".*");
  public static final String DEFAULT_INPUT_SET_EXPRESSION =
      EngineExpressionEvaluator.EXPR_START + "input" + EngineExpressionEvaluator.EXPR_END;
  public static final Pattern GENERIC_EXPRESSIONS_PATTERN = Pattern.compile(EngineExpressionEvaluator.EXPR_START_ESC
      + "([a-zA-Z]\\w*\\.?)*([a-zA-Z]\\w*)" + EngineExpressionEvaluator.EXPR_END_ESC);

  public boolean matchesInputSetPattern(String expression) {
    if (isEmpty(expression)) {
      return false;
    }
    return NGExpressionUtils.InputSetVariablePattern.matcher(expression).matches();
  }

  public boolean matchesPattern(Pattern pattern, String expression) {
    if (isEmpty(expression)) {
      return false;
    }
    return pattern.matcher(expression).matches();
  }

  // Function which matches pattern on given expression.
  public boolean containsPattern(Pattern pattern, String expression) {
    if (isEmpty(expression)) {
      return false;
    }
    return pattern.matcher(expression).find();
  }

  public String getInputSetValidatorPattern(String validatorName) {
    return "\\." + validatorName + "\\(";
  }

  public boolean isRuntimeOrExpressionField(String fieldValue) {
    return matchesInputSetPattern(fieldValue) || EngineExpressionEvaluator.hasExpressions(fieldValue);
  }

  public List<String> getListOfExpressions(String s) {
    Matcher matcher = GENERIC_EXPRESSIONS_PATTERN.matcher(s);
    List<String> allMatches = new ArrayList<>();
    while (matcher.find()) {
      allMatches.add(matcher.group());
    }
    return allMatches;
  }
}
