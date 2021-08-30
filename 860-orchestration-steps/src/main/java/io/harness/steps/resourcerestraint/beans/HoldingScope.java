package io.harness.steps.resourcerestraint.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@RecasterAlias("io.harness.steps.resourcerestraint.beans.HoldingScope")
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
