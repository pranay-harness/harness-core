package io.harness.pms.yaml.validation;

import static io.harness.data.structure.HasPredicate.hasNone;

import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExpressionUtils {
  public boolean matchesPattern(Pattern pattern, String expression) {
    if (hasNone(expression)) {
      return false;
    }
    return pattern.matcher(expression).matches();
  }

  public boolean containsPattern(Pattern pattern, String expression) {
    if (hasNone(expression)) {
      return false;
    }
    return pattern.matcher(expression).find();
  }
}
