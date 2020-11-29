package io.harness.pms.plan.creator.filters;

import io.harness.pms.filter.FilterCreationResponse;

public interface FilterCreationResponseMerger {
  void mergeFilterCreationResponse(FilterCreationResponse finalResponse, FilterCreationResponse current);
}
