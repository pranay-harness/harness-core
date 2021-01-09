package io.harness.pms.sdk.core.pipeline.filters;

import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.pipeline.filter.PipelineFilter;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.yaml.YamlField;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ChildrenFilterJsonCreator<T> implements FilterJsonCreator<T> {
  public abstract Map<String, YamlField> getDependencies(FilterCreationContext filterCreationContext);
  public abstract PipelineFilter getFilterForGivenField();

  public FilterCreationResponse handleNode(FilterCreationContext filterCreationContext, T field) {
    FilterCreationResponse response = FilterCreationResponse.builder().build();
    Map<String, YamlField> dependencies = getDependencies(filterCreationContext);
    response.addDependencies(dependencies);
    response.setPipelineFilter(getFilterForGivenField());
    // Note: Currently we treat that all the dependency fields are children but that might not be true.
    // Todo: Support for dependency not as direct children
    response.setStageCount(getStageCount(filterCreationContext, dependencies.values()));
    return response;
  }

  abstract int getStageCount(FilterCreationContext filterCreationContext, Collection<YamlField> children);
}
