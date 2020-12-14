package io.harness.pms.sdk.core.pipeline.filters;

import io.harness.plancreator.pipeline.PipelineInfoConfig;
import io.harness.pms.contracts.plan.EdgeLayoutList;
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
  public String getStartingNodeId(PipelineInfoConfig pipelineInfoConfig) {
    return pipelineInfoConfig.getUuid();
  }

  @Override
  public Map<String, GraphLayoutNode> createLayoutNodeMap(
      FilterCreationContext filterCreationContext, PipelineInfoConfig pipelineInfoConfig) {
    Map<String, GraphLayoutNode> layoutNodeMap = new HashMap<>();
    YamlField stagesYamlNode =
        Preconditions.checkNotNull(filterCreationContext.getCurrentField().getNode().getField("stages"));
    GraphLayoutNode pipelineGraph =
        GraphLayoutNode.newBuilder()
            .setNodeType("pipeline")
            .setNodeIdentifier("pipeline")
            .setNodeUUID(pipelineInfoConfig.getUuid())
            .setEdgeLayoutList(EdgeLayoutList.newBuilder().addNextIds(stagesYamlNode.getNode().getUuid()).build())
            .build();
    layoutNodeMap.put(pipelineInfoConfig.getUuid(), pipelineGraph);
    layoutNodeMap.putAll(StagesFilterJsonCreator.getStagesGraphLayoutNode(stagesYamlNode));
    return layoutNodeMap;
  }
}
