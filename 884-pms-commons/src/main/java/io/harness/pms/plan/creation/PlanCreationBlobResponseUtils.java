package io.harness.pms.plan.creation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.YamlException;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.contracts.plan.GraphLayoutInfo;
import io.harness.pms.contracts.plan.GraphLayoutNode;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.plan.YamlUpdates;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
@Slf4j
public class PlanCreationBlobResponseUtils {
  public void merge(PlanCreationBlobResponse.Builder builder, PlanCreationBlobResponse other) {
    if (other == null) {
      return;
    }
    addNodes(builder, other.getNodesMap());
    addDependencies(builder, other.getDeps());
    mergeStartingNodeId(builder, other.getStartingNodeId());
    mergeContext(builder, other.getContextMap());
    mergeLayoutNodeInfo(builder, other);
  }

  public PlanCreationBlobResponse mergeContext(
      PlanCreationBlobResponse.Builder builder, Map<String, PlanCreationContextValue> contextValueMap) {
    if (EmptyPredicate.isEmpty(contextValueMap)) {
      return builder.build();
    }
    builder.putAllContext(contextValueMap);
    return builder.build();
  }

  public PlanCreationBlobResponse addNodes(
      PlanCreationBlobResponse.Builder builder, Map<String, PlanNodeProto> newNodes) {
    if (EmptyPredicate.isEmpty(newNodes)) {
      return builder.build();
    }
    newNodes.values().forEach(newNode -> addNode(builder, newNode));
    return builder.build();
  }

  public PlanCreationBlobResponse addNode(PlanCreationBlobResponse.Builder builder, PlanNodeProto newNode) {
    // TODO: Add logic to update only if newNode has a more recent version.
    builder.putNodes(newNode.getUuid(), newNode);
    removeDependency(builder, newNode.getUuid());
    return builder.build();
  }

  public PlanCreationBlobResponse addDependencies(PlanCreationBlobResponse.Builder builder, Dependencies dependencies) {
    if (dependencies == null || EmptyPredicate.isEmpty(dependencies.getDependenciesMap())) {
      return builder.build();
    }
    dependencies.getDependenciesMap().forEach((key, value) -> addDependency(builder, key, value));
    builder.setDeps(builder.getDeps().toBuilder().setYaml(dependencies.getYaml()).build());
    return builder.build();
  }

  public PlanCreationBlobResponse addDependency(PlanCreationBlobResponse.Builder builder, String nodeId, String path) {
    if (builder.containsNodes(nodeId)) {
      return builder.build();
    }

    builder.setDeps(builder.getDeps().toBuilder().putDependencies(nodeId, path).build());
    return builder.build();
  }

  public PlanCreationBlobResponse removeDependency(PlanCreationBlobResponse.Builder builder, String nodeId) {
    builder.setDeps(builder.getDeps().toBuilder().removeDependencies(nodeId).build());
    return builder.build();
  }

  public void mergeStartingNodeId(PlanCreationBlobResponse.Builder builder, String otherStartingNodeId) {
    if (EmptyPredicate.isEmpty(otherStartingNodeId)) {
      return;
    }
    if (EmptyPredicate.isEmpty(builder.getStartingNodeId())) {
      builder.setStartingNodeId(otherStartingNodeId);
      return;
    }
    if (!builder.getStartingNodeId().equals(otherStartingNodeId)) {
      throw new InvalidRequestException(String.format(
          "Received different set of starting nodes: %s and %s", builder.getStartingNodeId(), otherStartingNodeId));
    }
  }

  public void mergeLayoutNodeInfo(PlanCreationBlobResponse.Builder builder, PlanCreationBlobResponse response) {
    if (response.getGraphLayoutInfo() != null) {
      String otherStartingNodeId = response.getGraphLayoutInfo().getStartingNodeId();
      GraphLayoutInfo.Builder layoutNodeInfo = GraphLayoutInfo.newBuilder();
      if (EmptyPredicate.isEmpty(builder.getGraphLayoutInfo().getStartingNodeId())) {
        layoutNodeInfo.setStartingNodeId(otherStartingNodeId);
      } else {
        layoutNodeInfo.setStartingNodeId(builder.getGraphLayoutInfo().getStartingNodeId());
      }
      Map<String, GraphLayoutNode> layoutMap = new HashMap<>();
      if (builder.getGraphLayoutInfo() != null) {
        layoutMap = builder.getGraphLayoutInfo().getLayoutNodesMap();
      }
      if (!(layoutMap instanceof HashMap)) {
        layoutMap = new HashMap<>(layoutMap);
      }
      if (response.getGraphLayoutInfo().getLayoutNodesMap() != null) {
        layoutMap.putAll(response.getGraphLayoutInfo().getLayoutNodesMap());
      }

      layoutNodeInfo.putAllLayoutNodes(layoutMap);
      builder.setGraphLayoutInfo(layoutNodeInfo);
    }
  }

  public PlanCreationBlobResponse mergeYamlUpdates(
      PlanCreationBlobResponse.Builder builder, YamlUpdates yamlUpdatesFromResponse) {
    String pipelineJson = builder.getDeps().getYaml();
    Map<String, String> fqnToJsonMap = yamlUpdatesFromResponse.getFqnToYamlMap();
    String updatedPipelineJson = mergeYamlUpdates(pipelineJson, fqnToJsonMap);
    builder.setDeps(builder.getDeps().toBuilder().setYaml(updatedPipelineJson).build());
    return builder.build();
  }

  public String mergeYamlUpdates(String pipelineJson, Map<String, String> fqnToJsonMap) {
    YamlNode pipelineNode;
    try {
      pipelineNode = YamlUtils.readTree(pipelineJson).getNode();
    } catch (IOException e) {
      log.error("Could not read the pipeline json:\n" + pipelineJson, e);
      throw new YamlException("Could not read the pipeline json");
    }
    fqnToJsonMap.keySet().forEach(fqn -> {
      try {
        pipelineNode.replacePath(fqn, YamlUtils.readTree(fqnToJsonMap.get(fqn)).getNode().getCurrJsonNode());
      } catch (IOException e) {
        log.error("Could not read json provided for the fqn: " + fqn + ". Json:\n" + fqnToJsonMap.get(fqn), e);
        throw new YamlException("Could not read json provided for the fqn: " + fqn);
      }
    });
    return pipelineNode.toString();
  }
}
