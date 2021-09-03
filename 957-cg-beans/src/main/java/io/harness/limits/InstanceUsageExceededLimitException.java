package io.harness.limits;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Value;

@OwnedBy(PL)
@Value
public class InstanceUsageExceededLimitException extends RuntimeException {
  private String accountId;
  private double usage;
  private String message;
}
