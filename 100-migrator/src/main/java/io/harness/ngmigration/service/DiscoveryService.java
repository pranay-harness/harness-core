/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.utils.NGYamlUtils;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.beans.NgEntityDetail;

import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.DiscoveryNode;
import software.wings.ngmigration.DiscoveryResult;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.ngmigration.NGYamlFile;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
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
    MutableGraph vizGraph = getGraphViz(entities, graph);
    try {
      Graphviz.fromGraph(vizGraph).render(Format.PNG).toFile(new File("/tmp/viz-output/viz.png"));
    } catch (IOException e) {
      log.warn("Unable to write visualization to file");
    }
    return DiscoveryResult.builder().entities(entities).links(graph).root(node.getEntityNode().getEntityId()).build();
  }

  public List<NGYamlFile> migrateEntity(MigrationInputDTO inputDTO, DiscoveryResult discoveryResult) {
    Map<CgEntityId, CgEntityNode> entities = discoveryResult.getEntities();
    Map<CgEntityId, Set<CgEntityId>> graph = discoveryResult.getLinks();
    CgEntityId root = discoveryResult.getRoot();
    Map<CgEntityId, NgEntityDetail> migratedEntities = new HashMap<>();

    Map<CgEntityId, Set<CgEntityId>> leafTracker =
        graph.entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> Sets.newHashSet(e.getValue())));
    List<NGYamlFile> ngYamlFiles = getAllYamlFiles(inputDTO, entities, graph, root, migratedEntities, leafTracker);

    // Write the files to ZIP folder
    try {
      FileUtils.cleanDirectory(new File("/tmp/zip-output"));
    } catch (IOException e) {
      log.warn("Failed to clean output directory");
    }
    File zipFile = new File("/tmp/zip-output/yamls.zip");
    zipFile.getParentFile().mkdirs();
    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile))) {
      for (NGYamlFile file : ngYamlFiles) {
        ZipEntry e = new ZipEntry(file.getFilename());
        out.putNextEntry(e);
        byte[] data = NGYamlUtils.getYamlString(file.getYaml()).getBytes();
        out.write(data, 0, data.length);
        out.closeEntry();
      }
    } catch (IOException e) {
      log.warn("Unable to save zip file");
    }
    return ngYamlFiles;
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

  private List<NGYamlFile> getAllYamlFiles(MigrationInputDTO inputDTO, Map<CgEntityId, CgEntityNode> entities,
      Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId, Map<CgEntityId, NgEntityDetail> migratedEntities,
      Map<CgEntityId, Set<CgEntityId>> leafTracker) {
    if (!leafTracker.containsKey(entityId) || isEmpty(leafTracker.get(entityId))) {
      return new ArrayList<>();
    }

    List<NGYamlFile> files = new ArrayList<>();
    while (isNotEmpty(leafTracker)) {
      List<CgEntityId> leafNodes = getLeafNodes(leafTracker);
      for (CgEntityId entry : leafNodes) {
        List<NGYamlFile> currentEntity =
            migrationFactory.getMethod(entry.getType()).getYamls(inputDTO, entities, graph, entry, migratedEntities);
        if (isNotEmpty(currentEntity)) {
          files.addAll(currentEntity);
        }
      }
      removeLeafNodes(leafTracker);
    }

    return files;
  }

  private MutableGraph getGraphViz(Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph) {
    MutableGraph vizGraph = Factory.mutGraph().setDirected(true);
    Map<CgEntityId, MutableNode> nodes = new HashMap<>();

    vizGraph.use((gr, ctx) -> {
      for (CgEntityId node : graph.keySet()) {
        NGMigrationEntity entityNode = entities.get(node).getEntity();
        MutableNode vizNode = Factory.mutNode(node.toString());
        vizNode.setName(
            Label.htmlLines(entityNode.getMigrationEntityName(), entityNode.getMigrationEntityType().name()));
        nodes.put(node, vizNode);
      }
      for (Map.Entry<CgEntityId, Set<CgEntityId>> entry : graph.entrySet()) {
        Set<CgEntityId> children = entry.getValue();
        MutableNode parentVizNode = nodes.get(entry.getKey());
        parentVizNode.addLink(children.stream().map(nodes::get).toArray(MutableNode[] ::new));
      }
    });

    vizGraph.add(nodes.values().toArray(new MutableNode[0]));
    return vizGraph;
  }
}
