package io.harness.ngmigration;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.DiscoveryNode;
import software.wings.ngmigration.DiscoveryResult;
import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.ngmigration.NGYamlFile;
import software.wings.ngmigration.NgMigration;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.CDC)
public class DiscoveryService {
  @Inject private NgMigrationFactory migrationFactory;

  private void travel(String accountId, String appId, Map<CgEntityId, CgEntityNode> entities,
      Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId parent, DiscoveryNode discoveryNode) {
    if (discoveryNode == null) {
      return;
    }
    CgEntityNode currentNode = discoveryNode.getEntityNode();
    Set<CgEntityId> chilldren = discoveryNode.getChildren();

    // Add the discovered node to the graph
    entities.putIfAbsent(currentNode.getEntityId(), currentNode);
    graph.putIfAbsent(currentNode.getEntityId(), new HashSet<>());

    // Link the discovered node to the parent
    if (parent != null) {
      // Note: parent will be null only the first time
      graph.get(parent).add(currentNode.getEntityId());
    }

    // Discover the child nodes and add to graph
    if (isNotEmpty(chilldren)) {
      chilldren.forEach(child -> {
        NgMigration ngMigration = migrationFactory.getMethod(child.getType());
        DiscoveryNode node = ngMigration.discover(accountId, appId, child.getId());
        travel(accountId, appId, entities, graph, currentNode.getEntityId(), node);
      });
    }
  }

  public DiscoveryResult discover(String accountId, String appId, String entityId, NGMigrationEntityType entityType) {
    Map<CgEntityId, CgEntityNode> entities = new HashMap<>();
    Map<CgEntityId, Set<CgEntityId>> graph = new HashMap<>();

    NgMigration ngMigration = migrationFactory.getMethod(entityType);
    DiscoveryNode node = ngMigration.discover(accountId, appId, entityId);
    if (node == null) {
      throw new IllegalStateException("Root cannot be found!");
    }
    travel(accountId, appId, entities, graph, null, node);
    return DiscoveryResult.builder().entities(entities).links(graph).root(node.getEntityNode().getEntityId()).build();
  }

  public List<NGYamlFile> migratePipeline(
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, String pipelineId) {
    return getAllYamlFiles(
        entities, graph, CgEntityId.builder().id(pipelineId).type(NGMigrationEntityType.PIPELINE).build());
  }

  private List<CgEntityId> getLeafNodes(Map<CgEntityId, Set<CgEntityId>> graph) {
    if (isEmpty(graph)) {
      return new ArrayList<>();
    }
    return graph.entrySet()
        .stream()
        .filter(entry -> isEmpty(entry.getValue()))
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  private void removeLeafNodes(Map<CgEntityId, Set<CgEntityId>> graph) {
    List<CgEntityId> leafNodes = getLeafNodes(graph);
    if (isEmpty(leafNodes)) {
      return;
    }
    leafNodes.forEach(graph::remove);
    if (isEmpty(graph)) {
      return;
    }
    for (Map.Entry<CgEntityId, Set<CgEntityId>> entry : graph.entrySet()) {
      if (isNotEmpty(entry.getValue())) {
        graph.get(entry.getKey()).removeAll(leafNodes);
      }
    }
  }

  private List<NGYamlFile> getAllYamlFiles(
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId) {
    if (!graph.containsKey(entityId) || isEmpty(graph.get(entityId))) {
      return new ArrayList<>();
    }

    List<NGYamlFile> files = new ArrayList<>();
    while (isNotEmpty(graph)) {
      List<CgEntityId> leafNodes = getLeafNodes(graph);
      for (CgEntityId entry : leafNodes) {
        List<NGYamlFile> currentEntity = migrationFactory.getMethod(entry.getType()).getYamls(entities, graph, entry);
        if (isNotEmpty(currentEntity)) {
          files.addAll(currentEntity);
        }
      }
      removeLeafNodes(graph);
    }

    return files;
  }
}
