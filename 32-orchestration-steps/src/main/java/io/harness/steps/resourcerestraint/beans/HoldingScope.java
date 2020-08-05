package io.harness.steps.resourcerestraint.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Value
@Builder
public class HoldingScope {
  @NotNull String scope;
  @NotNull String nodeSetupId;

  public static class HoldingScopeBuilder {
    private HoldingScopeBuilder() {}
    public static HoldingScopeBuilder aPlan() {
      return builder().scope("PLAN").nodeSetupId("");
    }
  }
}
