package software.wings.expression;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import lombok.Builder;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.lang.text.StrLookup;

import java.util.Map;

@Builder
public class EvaluateVariableResolver extends StrLookup {
  private Map<String, Object> context;
  private ExpressionEvaluator expressionEvaluator;
  private String objectPrefix;

  @Override
  public String lookup(String variable) {
    try {
      if (isNotEmpty(objectPrefix)) {
        variable = NormalizeVariableResolver.expand(variable, context, objectPrefix);
      }
      return String.valueOf(expressionEvaluator.evaluate(variable, context));
    } catch (JexlException exception) {
      // Suppress exception
    }
    return null;
  }
}
