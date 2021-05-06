package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;

@Value
@Builder
@OwnedBy(HarnessTeam.DX)
public class ScmFilterQueryTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  ScmConnector scmConnector;
  Set<Pair<String /* operator */, String /* standard */>> conditions;
  // PR
  int prNumber;
  // push (or range of commits)
  String branch;
  String latestCommit;
  String previousCommit;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return null;
  }
}
