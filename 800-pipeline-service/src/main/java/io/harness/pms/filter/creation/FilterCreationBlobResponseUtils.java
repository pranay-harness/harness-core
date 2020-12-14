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
    mergeStartingNodeId(builder, response.getResponse());
    mergeResolvedDependencies(builder, response.getResponse());
    mergeDependencies(builder, response.getResponse());
    mergeFilters(response, filters);
    updateStageCount(builder, response.getResponse());
    mergeLayoutNodeMap(builder, response.getResponse());
  }

  public void mergeStartingNodeId(FilterCreationBlobResponse.Builder builder, FilterCreationBlobResponse response) {
    if (EmptyPredicate.isNotEmpty(response.getStartingNodeId())) {
      builder.setStartingNodeId(response.getStartingNodeId());
    }
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

  public void mergeLayoutNodeMap(FilterCreationBlobResponse.Builder builder, FilterCreationBlobResponse response) {
    if (isNotEmpty(response.getLayoutNodesMap())) {
      response.getLayoutNodesMap().forEach(builder::putLayoutNodes);
    }
  }
}
