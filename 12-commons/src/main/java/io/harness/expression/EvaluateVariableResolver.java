package io.harness.expression;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.exception.FunctorException;
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
  private VariableResolverTracker variableResolverTracker;

  @Override
  public String lookup(String variable) {
    String name = prefix + ++varIndex + suffix;
    try {
      if (isNotEmpty(objectPrefixes)) {
        variable = NormalizeVariableResolver.expand(variable, context, objectPrefixes);
      }
      final Object evaluated = expressionEvaluator.evaluate(variable, context);
      context.set(name, evaluated);
      if (variableResolverTracker != null && evaluated != null) {
        variableResolverTracker.observed(variable, context.get(name));
      }
    } catch (JexlException exception) {
      if (exception.getCause() instanceof FunctorException) {
        throw(FunctorException) exception.getCause();
      }
      context.set(name, "${" + variable + "}");
    }
    return name;
  }
}
