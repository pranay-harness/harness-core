package io.harness.pms.plan.creation;

import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.sdk.core.pipeline.filters.FilterCreationResponseMerger;

public class PipelineServiceFilterCreationResponseMerger implements FilterCreationResponseMerger {
  @Override
  public void mergeFilterCreationResponse(FilterCreationResponse finalResponse, FilterCreationResponse current) {
    // nothing to do
  }
}
