package io.harness.pms.sdk.core.execution.expression;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class DummyFunctor implements SdkFunctor {
  public String get(String expression) {
    return "DummyResponse";
  }
}
