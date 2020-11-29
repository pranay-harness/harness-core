package io.harness.pms.sample.cv.creator.filters;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.pms.filter.FilterCreationResponse;
import io.harness.pms.plan.creator.filters.FilterCreationResponseMerger;

public class CVFilterCreationResponseMerger implements FilterCreationResponseMerger {
  @Override
  public void mergeFilterCreationResponse(FilterCreationResponse finalResponse, FilterCreationResponse current) {
    if (current == null || current.getPipelineFilter() == null) {
      return;
    }

    if (finalResponse.getPipelineFilter() == null) {
      finalResponse.setPipelineFilter(CvFilter.builder().build());
    }
  }
}
