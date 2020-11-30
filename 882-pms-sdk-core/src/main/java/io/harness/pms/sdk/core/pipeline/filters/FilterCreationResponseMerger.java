package io.harness.pms.sdk.core.pipeline.filters;

import io.harness.pms.pipeline.filter.FilterCreationResponse;

public interface FilterCreationResponseMerger {
  void mergeFilterCreationResponse(FilterCreationResponse finalResponse, FilterCreationResponse current);
}
