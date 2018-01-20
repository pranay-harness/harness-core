package software.wings.expression;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import lombok.Builder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.text.StrLookup;

@Builder
public class EvaluateVariableResolver extends StrLookup {
  private JexlContext context;
  private ExpressionEvaluator expressionEvaluator;
  private String objectPrefix;

  @Override
  public String lookup(String variable) {
    try {
      if (isNotEmpty(objectPrefix)) {
        variable = NormalizeVariableResolver.expand(variable, context, objectPrefix);
      }
      return String.valueOf(expressionEvaluator.evaluate(variable, context, objectPrefix));
    } catch (JexlException exception) {
    }
    return null;
  }
}
