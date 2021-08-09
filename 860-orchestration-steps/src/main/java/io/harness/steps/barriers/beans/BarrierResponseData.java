package io.harness.steps.barriers.beans;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.tasks.ResponseData;

import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class BarrierResponseData implements ResponseData {
  boolean failed;
  BarrierError barrierError;

  @OwnedBy(PIPELINE)
  @Value
  @Builder
  public static class BarrierError {
    boolean timedOut;
    String errorMessage;
  }
}
