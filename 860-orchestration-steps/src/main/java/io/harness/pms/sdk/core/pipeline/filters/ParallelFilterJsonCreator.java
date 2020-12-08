package io.harness.pms.sdk.core.pipeline.filters;

import io.harness.pms.pipeline.filter.PipelineFilter;
import io.harness.pms.plan.EdgeLayoutList;
import io.harness.pms.plan.GraphLayoutNode;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.yaml.YamlField;

import java.util.*;
import java.util.stream.Collectors;

public class ParallelFilterJsonCreator extends ChildrenFilterJsonCreator<YamlField> {
  @Override
  public Map<String, YamlField> getDependencies(FilterCreationContext filterCreationContext) {
    return Optional.of(filterCreationContext.getCurrentField().getNode().asArray())
        .orElse(Collections.emptyList())
        .stream()
        .map(el -> el.getField("stage"))
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(field -> field.getNode().getUuid(), field -> field));
  }

  @Override
  public PipelineFilter getFilterForGivenField() {
    return null;
  }

  @Override
  public String getStartingNodeId(YamlField field) {
    return null;
  }

  @Override
  public Class<YamlField> getFieldClass() {
    return YamlField.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("parallel", Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  @Override
  public Map<String, GraphLayoutNode> createLayoutNodeMap(
      FilterCreationContext filterCreationContext, YamlField yamlField) {
    List<String> possibleSiblings = new ArrayList<>();
    possibleSiblings.add("stage");
    possibleSiblings.add("parallel");
    YamlField nextSibling =
        filterCreationContext.getCurrentField().getNode().nextSiblingFromParentArray("parallel", possibleSiblings);

    String nextSiblingUuid = nextSibling == null ? null : nextSibling.getNode().getUuid();

    Map<String, YamlField> children = getDependencies(filterCreationContext);
    List<String> childrenUuids = new ArrayList<>(children.keySet());
    EdgeLayoutList.Builder stagesEdgesBuilder = EdgeLayoutList.newBuilder().addAllCurrentNodeChildren(childrenUuids);
    Map<String, GraphLayoutNode> layoutNodeMap = children.values().stream().collect(Collectors.toMap(stageField
        -> stageField.getNode().getUuid(),
        stageField
        -> GraphLayoutNode.newBuilder()
               .setNodeUUID(stageField.getNode().getUuid())
               .setNodeType("stage")
               .setNodeIdentifier(stageField.getNode().getIdentifier())
               .setEdgeLayoutList(nextSiblingUuid != null
                       ? EdgeLayoutList.newBuilder().addNextIds(nextSiblingUuid).build()
                       : EdgeLayoutList.newBuilder().build())
               .build()));
    GraphLayoutNode parallelNode = GraphLayoutNode.newBuilder()
                                       .setNodeUUID(yamlField.getNode().getUuid())
                                       .setNodeType("parallel")
                                       .setNodeIdentifier("parallel")
                                       .setEdgeLayoutList(stagesEdgesBuilder.build())
                                       .build();
    layoutNodeMap.put(yamlField.getNode().getUuid(), parallelNode);
    return layoutNodeMap;
  }
}
