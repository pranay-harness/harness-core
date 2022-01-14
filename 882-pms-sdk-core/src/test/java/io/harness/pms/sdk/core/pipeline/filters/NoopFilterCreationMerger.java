package io.harness.pms.sdk.core.pipeline.filters;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.filter.creation.FilterCreationResponse;

@OwnedBy(HarnessTeam.PIPELINE)
public class NoopFilterCreationMerger implements FilterCreationResponseMerger {
  @Override
  public void mergeFilterCreationResponse(FilterCreationResponse finalResponse, FilterCreationResponse current) {
    // Do Nothing
  }
}
