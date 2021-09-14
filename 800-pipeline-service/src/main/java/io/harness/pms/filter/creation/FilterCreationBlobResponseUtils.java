/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.filter.creation;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.pms.contracts.plan.FilterCreationBlobResponse;

import java.util.ArrayList;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FilterCreationBlobResponseUtils {
  public void mergeResponses(
      FilterCreationBlobResponse.Builder builder, FilterCreationResponseWrapper response, Map<String, String> filters) {
    if (response == null || response.getResponse() == null) {
      return;
    }
    mergeResolvedDependencies(builder, response.getResponse());
    mergeDependencies(builder, response.getResponse());
    mergeFilters(response, filters);
    updateStageCount(builder, response.getResponse());
    mergeReferredEntities(builder, response.getResponse());
    mergeStageNames(builder, response.getResponse());
  }

  public void updateStageCount(
      FilterCreationBlobResponse.Builder builder, FilterCreationBlobResponse filterCreationBlobResponse) {
    builder.setStageCount(builder.getStageCount() + filterCreationBlobResponse.getStageCount());
  }

  public void mergeFilters(FilterCreationResponseWrapper response, Map<String, String> filters) {
    if (isNotEmpty(response.getResponse().getFilter())) {
      filters.put(response.getServiceName(), response.getResponse().getFilter());
    }
  }

  public void mergeReferredEntities(FilterCreationBlobResponse.Builder builder, FilterCreationBlobResponse response) {
    if (isNotEmpty(response.getReferredEntitiesList())) {
      builder.addAllReferredEntities(response.getReferredEntitiesList());
    }
  }

  public void mergeStageNames(FilterCreationBlobResponse.Builder builder, FilterCreationBlobResponse response) {
    if (response.getStageNamesList() != null) {
      builder.addAllStageNames(new ArrayList<>(response.getStageNamesList()));
    }
  }

  public void mergeResolvedDependencies(
      FilterCreationBlobResponse.Builder builder, FilterCreationBlobResponse response) {
    if (isNotEmpty(response.getResolvedDependenciesMap())) {
      response.getResolvedDependenciesMap().forEach((key, value) -> {
        builder.putResolvedDependencies(key, value);
        builder.removeDependencies(key);
      });
    }
  }

  public void mergeDependencies(FilterCreationBlobResponse.Builder builder, FilterCreationBlobResponse response) {
    if (isNotEmpty(response.getDependenciesMap())) {
      response.getDependenciesMap().forEach((key, value) -> {
        if (!builder.containsResolvedDependencies(key)) {
          builder.putDependencies(key, value);
        }
      });
    }
  }
}
