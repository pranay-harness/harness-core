package io.harness.ci.plan.creator.filter;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.sdk.core.pipeline.filters.FilterCreationResponseMerger;

public class CIFilterCreationResponseMerger implements FilterCreationResponseMerger {
  @Override
  public void mergeFilterCreationResponse(FilterCreationResponse finalResponse, FilterCreationResponse current) {
    if (current == null || current.getPipelineFilter() == null) {
      return;
    }

    if (finalResponse.getPipelineFilter() == null) {
      finalResponse.setPipelineFilter(CIFilter.builder().build());
    }

    CIFilter finalCIFilter = (CIFilter) finalResponse.getPipelineFilter();
    CIFilter currentCIFilter = (CIFilter) current.getPipelineFilter();

    if (isNotEmpty(currentCIFilter.getRepoNames())) {
      finalCIFilter.addRepoNames(currentCIFilter.getRepoNames());
    }
  }
}
