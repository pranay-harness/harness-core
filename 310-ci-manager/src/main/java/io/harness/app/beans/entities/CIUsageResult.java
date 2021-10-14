package io.harness.app.beans.entities;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CIUsageResult {
  private String accountIdentifier;
  private String module;
  private long timestamp;
  private CIUsageActiveCommitters activeCommitters;
}
