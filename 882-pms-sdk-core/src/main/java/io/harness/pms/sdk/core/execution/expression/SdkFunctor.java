package io.harness.pms.sdk.core.execution.expression;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;

@OwnedBy(HarnessTeam.PIPELINE)
public interface SdkFunctor {
  ExpressionResult get(Ambiance ambiance, String... args);
}
