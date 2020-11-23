package io.harness.pms.sdk.creator.filters;

import io.harness.pms.filter.FilterCreationResponse;

public interface FilterCreationResponseMerger {
  void mergeFilterCreationResponse(FilterCreationResponse finalResponse, FilterCreationResponse current);
}
