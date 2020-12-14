package io.harness.pms.sdk.core.pipeline.filters;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.plan.GraphLayoutNode;
import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.pipeline.filter.PipelineFilter;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.yaml.YamlField;

import java.util.HashMap;
import java.util.Map;

public abstract class ChildrenFilterJsonCreator<T> implements FilterJsonCreator<T> {
  public abstract Map<String, YamlField> getDependencies(FilterCreationContext filterCreationContext);
  public abstract PipelineFilter getFilterForGivenField();

  public FilterCreationResponse handleNode(FilterCreationContext filterCreationContext, T field) {
    FilterCreationResponse response = FilterCreationResponse.builder().build();
    response.addDependencies(getDependencies(filterCreationContext));
    response.setPipelineFilter(getFilterForGivenField());
    response.setLayoutNodes(createLayoutNodeMap(filterCreationContext, field));
    if (EmptyPredicate.isNotEmpty(getStartingNodeId(field))) {
      response.setStartingNodeId(getStartingNodeId(field));
    }
    return response;
  }

  public Map<String, GraphLayoutNode> createLayoutNodeMap(FilterCreationContext filterCreationContext, T yamlField) {
    return new HashMap<>();
  }

  abstract public String getStartingNodeId(T field);
}
