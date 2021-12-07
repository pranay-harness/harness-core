package io.harness.pms.sdk.core.governance;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.governance.ExpansionPlacement;

import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class ExpansionResponse {
  String key;
  String value;
  ExpansionPlacement placement;
  boolean success;
  String errorMessage;
}
