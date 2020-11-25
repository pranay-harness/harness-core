package io.harness.cdng.pipeline.plancreators;

import static java.lang.String.format;

import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.expression.ExpressionEvaluator;
import io.harness.pms.ambiance.Ambiance;
import io.harness.yaml.core.StepGroupElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.annotation.Nonnull;

@Singleton
public class PlanCreatorHelper {
  @Inject private EngineExpressionService engineExpressionService;

  public boolean shouldNodeRun(@Nonnull RollbackNode rollbackNode, @Nonnull Ambiance ambiance) {
    if (rollbackNode.isShouldAlwaysRun()) {
      return true;
    }
    String value = engineExpressionService.renderExpression(
        ambiance, format("${%s.status}", rollbackNode.getDependentNodeIdentifier()));

    return !ExpressionEvaluator.containsVariablePattern(value);
  }

  public static boolean isStepGroupWithRollbacks(@Nonnull ExecutionWrapper executionWrapper) {
    return executionWrapper instanceof StepGroupElement
        && EmptyPredicate.isNotEmpty(((StepGroupElement) executionWrapper).getRollbackSteps());
  }
}
