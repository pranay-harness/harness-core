/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sample.cd.creator.filters;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

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

    if (isNotEmpty(currentCdFilter.getDeploymentTypes())) {
      finalCdFilter.addDeploymentTypes(currentCdFilter.getDeploymentTypes());
    }

    if (isNotEmpty(currentCdFilter.getEnvironmentNames())) {
      finalCdFilter.addEnvironmentNames(currentCdFilter.getEnvironmentNames());
    }

    if (isNotEmpty(currentCdFilter.getServiceNames())) {
      finalCdFilter.addServiceNames(currentCdFilter.getServiceNames());
    }
  }
}
