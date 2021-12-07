package io.harness.pms.sdk.core.governance;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class ExpansionResponse {
  String key;
  String value;
  // todo: add the enum for expansion placement strategy here when the proto file is defined
  boolean success;
  String errorMessage;
}
