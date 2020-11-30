package io.harness.pms.sample.cv.creator.filters;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.pms.pipeline.filter.FilterCreationResponse;
import io.harness.pms.sdk.core.pipeline.filters.FilterCreationResponseMerger;

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
