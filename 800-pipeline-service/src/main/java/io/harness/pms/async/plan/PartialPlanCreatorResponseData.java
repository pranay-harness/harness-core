package io.harness.pms.async.plan;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.PartialPlanResponse;
import io.harness.tasks.ResponseData;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class PartialPlanCreatorResponseData implements ResponseData {
  PartialPlanResponse partialPlanResponse;
}
