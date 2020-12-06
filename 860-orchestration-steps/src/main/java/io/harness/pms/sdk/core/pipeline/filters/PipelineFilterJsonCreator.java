package io.harness.pms.sdk.core.pipeline.filters;

import io.harness.plancreator.pipeline.PipelineInfoConfig;
import io.harness.pms.pipeline.filter.PipelineFilter;
import io.harness.pms.plan.EdgeLayoutList;
import io.harness.pms.plan.GraphLayoutNode;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.yaml.YamlField;

import com.google.common.base.Preconditions;
import java.util.*;
import java.util.stream.Collectors;

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
    Map<String, YamlField> dependencies = new HashMap<>();
    YamlField stagesYamlNode =
        Preconditions.checkNotNull(filterCreationContext.getCurrentField().getNode().getField("stages"));
    if (stagesYamlNode.getNode() == null) {
      return dependencies;
    }
    List<YamlField> stageYamlFields = getStageYamlFields(stagesYamlNode);
    for (YamlField stageYamlField : stageYamlFields) {
      dependencies.put(stageYamlField.getNode().getUuid(), stageYamlField);
    }
    return dependencies;
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
    Map<String, YamlField> stageYamlFieldMap = new HashMap<>();
    EdgeLayoutList.Builder stagesEdgesBuilder = EdgeLayoutList.newBuilder();
    getStageYamlFields(stagesYamlNode).forEach(stageField -> {
      stageYamlFieldMap.put(stageField.getNode().getUuid(), stageField);
      stagesEdgesBuilder.addNextIds(stageField.getNode().getUuid());
    });
    GraphLayoutNode pipelineGraph =
        GraphLayoutNode.newBuilder()
            .setNodeType("pipeline")
            .setNodeIdentifier("pipeline")
            .setNodeUUID(pipelineInfoConfig.getUuid())
            .setEdgeLayoutList(EdgeLayoutList.newBuilder().addNextIds(stagesYamlNode.getNode().getUuid()).build())
            .build();
    GraphLayoutNode stagesGraph = GraphLayoutNode.newBuilder()
                                      .setNodeUUID(stagesYamlNode.getNode().getUuid())
                                      .setNodeType("stages")
                                      .setNodeIdentifier("stages")
                                      .setEdgeLayoutList(stagesEdgesBuilder.build())
                                      .build();
    layoutNodeMap.put(pipelineInfoConfig.getUuid(), pipelineGraph);
    layoutNodeMap.put(stagesYamlNode.getNode().getUuid(), stagesGraph);
    layoutNodeMap.put(pipelineInfoConfig.getUuid(), pipelineGraph);
    return layoutNodeMap;
  }

  private List<YamlField> getStageYamlFields(YamlField stagesYamlField) {
    List<YamlField> stageYamlFields = Optional.of(stagesYamlField.getNode().asArray())
                                          .orElse(Collections.emptyList())
                                          .stream()
                                          .map(el -> el.getField("stage"))
                                          .filter(Objects::nonNull)
                                          .collect(Collectors.toList());
    stageYamlFields.addAll(Optional.of(stagesYamlField.getNode().asArray())
                               .orElse(Collections.emptyList())
                               .stream()
                               .map(el -> el.getField("parallel"))
                               .filter(Objects::nonNull)
                               .collect(Collectors.toList()));
    return stageYamlFields;
  }
}
