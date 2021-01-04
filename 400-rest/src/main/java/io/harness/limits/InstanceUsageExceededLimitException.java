package io.harness.limits;

import lombok.Value;

@Value
public class InstanceUsageExceededLimitException extends RuntimeException {
  private String accountId;
  private double usage;
  private String message;
}
