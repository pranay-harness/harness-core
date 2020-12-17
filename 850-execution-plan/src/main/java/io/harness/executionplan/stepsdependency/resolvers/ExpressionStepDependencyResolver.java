package io.harness.executionplan.stepsdependency.resolvers;

import io.harness.executionplan.stepsdependency.KeyAware;
import io.harness.executionplan.stepsdependency.StepDependencyResolver;
import io.harness.executionplan.stepsdependency.StepDependencyResolverContext;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.pms.expression.EngineExpressionService;

import com.google.inject.Inject;
import java.util.Optional;

/**
 * This class resolves all outcomes, sweeping output, expressions, input args according to precedence order defined in
 * workflow engine.
 */
public class ExpressionStepDependencyResolver implements StepDependencyResolver {
  @Inject private EngineExpressionService engineExpressionService;

  @Override
  public <T> Optional<T> resolve(StepDependencySpec spec, StepDependencyResolverContext resolverContext) {
    if (spec instanceof KeyAware) {
      KeyAware keyAware = (KeyAware) spec;
      return Optional.ofNullable(
          (T) engineExpressionService.evaluateExpression(resolverContext.getAmbiance(), keyAware.getKey()));
    }
    return Optional.empty();
  }
}
