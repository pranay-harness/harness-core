package expressions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.expressions.AmbianceExpressionEvaluator;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.expression.VariableResolverTracker;
import io.harness.pms.contracts.ambiance.Ambiance;

import java.util.Set;

@OwnedBy(HarnessTeam.CI)
public class CIExpressionEvaluator extends AmbianceExpressionEvaluator {
  public CIExpressionEvaluator(VariableResolverTracker variableResolverTracker, Ambiance ambiance,
      Set<NodeExecutionEntityType> entityTypes, boolean refObjectSpecific) {
    super(variableResolverTracker, ambiance, entityTypes, refObjectSpecific);
  }
}
