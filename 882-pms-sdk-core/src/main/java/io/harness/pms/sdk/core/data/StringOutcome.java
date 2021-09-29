package io.harness.pms.sdk.core.data;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(PIPELINE)
@RecasterAlias("io.harness.pms.sdk.core.data.StringOutcome")
public class StringOutcome implements Outcome {
  String message;
}
