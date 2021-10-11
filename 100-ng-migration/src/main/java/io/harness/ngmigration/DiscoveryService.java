package io.harness.ngmigration;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class DiscoveryService {
  @Inject private NgMigrationFactory migrationFactory;
  @Inject private PipelineMigrationService pipelineMigrationService;

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
    return pipelineMigrationService.getYamls(
        entities, graph, CgEntityId.builder().id(pipelineId).type(NGMigrationEntityType.PIPELINE).build());
  }
}
