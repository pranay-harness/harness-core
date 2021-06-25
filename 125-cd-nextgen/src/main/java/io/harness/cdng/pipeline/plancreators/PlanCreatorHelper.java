package io.harness.cdng.pipeline.plancreators;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.data.structure.EmptyPredicate;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.yaml.core.StepGroupElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.annotation.Nonnull;

@Singleton
@OwnedBy(HarnessTeam.CDC)
public class PlanCreatorHelper {
  @Inject private EngineExpressionService engineExpressionService;

  public boolean shouldNodeRun(@Nonnull RollbackNode rollbackNode, @Nonnull Ambiance ambiance) {
    if (rollbackNode.isShouldAlwaysRun()) {
      return true;
    }

    String value = engineExpressionService.renderExpression(
        ambiance, format("<+%s.status>", rollbackNode.getDependentNodeIdentifier()), true);
    return !EngineExpressionEvaluator.hasExpressions(value);
  }

  public static boolean isStepGroupWithRollbacks(@Nonnull ExecutionWrapper executionWrapper) {
    return executionWrapper instanceof StepGroupElement
        && EmptyPredicate.isNotEmpty(((StepGroupElement) executionWrapper).getRollbackSteps());
  }
}
