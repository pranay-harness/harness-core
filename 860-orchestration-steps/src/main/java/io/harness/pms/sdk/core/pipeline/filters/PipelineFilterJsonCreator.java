package io.harness.pms.sdk.core.pipeline.filters;

import io.harness.plancreator.pipeline.PipelineInfoConfig;
import io.harness.pms.contracts.plan.GraphLayoutNode;
import io.harness.pms.pipeline.filter.PipelineFilter;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.yaml.YamlField;

import com.google.common.base.Preconditions;
import java.util.*;

public class PipelineFilterJsonCreator extends ChildrenFilterJsonCreator<PipelineInfoConfig> {
  @Override
  public Class<PipelineInfoConfig> getFieldClass() {
    return PipelineInfoConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("pipeline", Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  @Override
  public Map<String, YamlField> getDependencies(FilterCreationContext filterCreationContext) {
    YamlField stagesYamlNode =
        Preconditions.checkNotNull(filterCreationContext.getCurrentField().getNode().getField("stages"));
    return StagesFilterJsonCreator.getDependencies(stagesYamlNode);
  }

  @Override
  public PipelineFilter getFilterForGivenField() {
    return null;
  }

  @Override
  public String getStartingNodeId(FilterCreationContext filterCreationContext, PipelineInfoConfig pipelineInfoConfig) {
    return StagesFilterJsonCreator.getStartingNodeId(
        Preconditions.checkNotNull(filterCreationContext.getCurrentField().getNode().getField("stages")));
  }

  @Override
  public Map<String, GraphLayoutNode> createLayoutNodeMap(
      FilterCreationContext filterCreationContext, PipelineInfoConfig pipelineInfoConfig) {
    Map<String, GraphLayoutNode> layoutNodeMap = new HashMap<>();
    YamlField stagesYamlNode =
        Preconditions.checkNotNull(filterCreationContext.getCurrentField().getNode().getField("stages"));
    layoutNodeMap.putAll(StagesFilterJsonCreator.getStagesGraphLayoutNode(stagesYamlNode));
    return layoutNodeMap;
  }
}
