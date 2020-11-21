package io.harness.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StrLookup;

@OwnedBy(CDC)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EngineVariableResolver extends StrLookup<Object> {
  EngineExpressionEvaluator expressionEvaluator;
  VariableResolverTracker variableResolverTracker;
  EngineJexlContext ctx;
  String prefix;
  String suffix;
  @NonFinal int varIndex;

  @Builder
  public EngineVariableResolver(
      EngineExpressionEvaluator expressionEvaluator, EngineJexlContext ctx, String prefix, String suffix) {
    this.expressionEvaluator = expressionEvaluator;
    this.variableResolverTracker = expressionEvaluator.getVariableResolverTracker();
    this.ctx = ctx;
    this.prefix = prefix;
    this.suffix = suffix;
    this.varIndex = 0;
  }

  @Override
  public String lookup(String variable) {
    String name = prefix + ++varIndex + suffix;
    Pair<Object, Boolean> evaluated = expressionEvaluator.evaluateVariable(variable, ctx);
    ctx.set(name, evaluated.getLeft());
    if (evaluated.getRight() && evaluated.getLeft() != null && variableResolverTracker != null) {
      variableResolverTracker.observed(variable, evaluated.getLeft());
    }
    return name;
  }
}
