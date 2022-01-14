package io.harness.delegate.beans.executioncapability;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.expression.ExpressionEvaluator;

import java.util.List;

@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public interface ExecutionCapabilityDemander {
  List<io.harness.delegate.beans.executioncapability.ExecutionCapability> fetchRequiredExecutionCapabilities(
      ExpressionEvaluator maskingEvaluator);
}
