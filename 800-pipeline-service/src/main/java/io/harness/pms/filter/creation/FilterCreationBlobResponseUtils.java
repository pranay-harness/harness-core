package io.harness.pms.filter.creation;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.plan.FilterCreationBlobResponse;

import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FilterCreationBlobResponseUtils {
  public void mergeResponses(
      FilterCreationBlobResponse.Builder builder, FilterCreationResponseWrapper response, Map<String, String> filters) {
    if (response == null) {
      return;
    }
    mergeResolvedDependencies(builder, response.getResponse());
    mergeDependencies(builder, response.getResponse());
    mergeFilters(response, filters);
    updateStageCount(builder, response.getResponse());
  }

  public void updateStageCount(
      FilterCreationBlobResponse.Builder builder, FilterCreationBlobResponse filterCreationBlobResponse) {
    builder.setStageCount(filterCreationBlobResponse.getStageCount());
  }

  public void mergeFilters(FilterCreationResponseWrapper response, Map<String, String> filters) {
    if (isNotEmpty(response.getResponse().getFilter())) {
      filters.put(response.getServiceName(), response.getResponse().getFilter());
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
