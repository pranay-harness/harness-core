package io.harness.pms.sdk.core.governance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class ExpansionResponse {
  String fqn;
  JsonExpansion expansion;
  boolean success;
  String errorMessage;
}
