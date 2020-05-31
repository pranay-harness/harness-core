package io.harness.expression;

import io.harness.exception.FunctorException;
import lombok.Builder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.text.StrLookup;

@Builder
public class EvaluateVariableResolver extends StrLookup {
  private JexlContext context;
  private ExpressionEvaluatorItfc expressionEvaluator;
  private int varIndex;
  private String prefix;
  private String suffix;
  private VariableResolverTracker variableResolverTracker;

  @Override
  public String lookup(String variable) {
    String name = prefix + ++varIndex + suffix;
    try {
      final Object evaluated = expressionEvaluator.evaluate(variable, context);
      context.set(name, evaluated);
      if (variableResolverTracker != null && evaluated != null) {
        variableResolverTracker.observed(variable, context.get(name));
      }
    } catch (JexlException exception) {
      if (exception.getCause() instanceof FunctorException) {
        FunctorException functorException = (FunctorException) exception.getCause();
        functorException.addParam(FunctorException.EXPRESSION_ARG, variable);
        throw functorException;
      }
      context.set(name, "${" + variable + "}");
    }
    return name;
  }
}
