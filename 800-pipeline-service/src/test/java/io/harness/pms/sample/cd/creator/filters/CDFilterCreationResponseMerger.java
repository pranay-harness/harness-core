package io.harness.pms.sample.cd.creator.filters;

import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.sdk.core.pipeline.filters.FilterCreationResponseMerger;

public class CDFilterCreationResponseMerger implements FilterCreationResponseMerger {
  @Override
  public void mergeFilterCreationResponse(FilterCreationResponse finalResponse, FilterCreationResponse current) {
    if (current == null || current.getPipelineFilter() == null) {
      return;
    }

    if (finalResponse.getPipelineFilter() == null) {
      finalResponse.setPipelineFilter(CdFilter.builder().build());
    }

    CdFilter finalCdFilter = (CdFilter) finalResponse.getPipelineFilter();
    CdFilter currentCdFilter = (CdFilter) current.getPipelineFilter();

    if (hasSome(currentCdFilter.getDeploymentTypes())) {
      finalCdFilter.addDeploymentTypes(currentCdFilter.getDeploymentTypes());
    }

    if (hasSome(currentCdFilter.getEnvironmentNames())) {
      finalCdFilter.addEnvironmentNames(currentCdFilter.getEnvironmentNames());
    }

    if (hasSome(currentCdFilter.getServiceNames())) {
      finalCdFilter.addServiceNames(currentCdFilter.getServiceNames());
    }
  }
}
