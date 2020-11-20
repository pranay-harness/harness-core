package io.harness.steps.barriers.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.tasks.ResponseData;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class BarrierResponseData implements ResponseData {
  boolean failed;
  String errorMessage;
}
