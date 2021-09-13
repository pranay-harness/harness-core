package io.harness.pms.cdng.execution.expression;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.execution.expression.SdkFunctor;

// Do Not delete this class. Testing the functors.
@OwnedBy(HarnessTeam.PIPELINE)
public class DummyFunctor implements SdkFunctor {
  public String get(String expression) {
    return "DummyResponse";
  }
}
