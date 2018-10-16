package io.harness.expression;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import lombok.Builder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.text.StrLookup;

import java.util.List;

@Builder
public class EvaluateVariableResolver extends StrLookup {
  private JexlContext context;
  private ExpressionEvaluator expressionEvaluator;
  private List<String> objectPrefixes;
  private int varIndex;
  private String prefix;
  private String suffix;

  @Override
  public String lookup(String variable) {
    Object value = null;
    try {
      if (isNotEmpty(objectPrefixes)) {
        variable = NormalizeVariableResolver.expand(variable, context, objectPrefixes);
      }
      value = expressionEvaluator.evaluate(variable, context);
    } catch (JexlException exception) {
      value = "${" + variable + "}";
    }

    String name = prefix + ++varIndex + suffix;
    context.set(name, value);
    return name;
  }
}
