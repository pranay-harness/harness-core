package io.harness.limits;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Value;

@OwnedBy(PL)
@Value
@TargetModule(HarnessModule._957_CG_BEANS)
public class InstanceUsageExceededLimitException extends RuntimeException {
  private String accountId;
  private double usage;
  private String message;
}
