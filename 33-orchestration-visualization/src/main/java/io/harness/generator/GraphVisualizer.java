package io.harness.generator;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;

import guru.nidi.graphviz.attribute.MapAttributes;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.Renderer;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import guru.nidi.graphviz.rough.FillStyle;
import guru.nidi.graphviz.rough.RoughFilter;
import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.beans.EdgeList;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.dto.OrchestrationGraphDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ExcludeRedesign
@Slf4j
public class GraphVisualizer {
  private static final String BASE_DIRECTORY = "resources/graphviz/";

  public void generateImage(OrchestrationGraphDTO graph, OutputStream output) throws IOException {
    MutableGraph mutableGraph = generateGraph(graph, graph.getRootNodeIds());
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      buildImageFromMutableGraph(mutableGraph).toOutputStream(os);
      output.write(os.toByteArray(), 0, os.size());
      output.flush();
    }
  }

  public void generateImage(OrchestrationGraphDTO graph, String filename) throws IOException {
    MutableGraph mutableGraph = generateGraph(graph, graph.getRootNodeIds());
    addLinksToGraph(mutableGraph, graph);

    buildImageFromMutableGraph(mutableGraph).toFile(new File(BASE_DIRECTORY, FilenameUtils.getName(filename)));
  }

  private MutableGraph generateGraph(OrchestrationGraphDTO graph, List<String> nodeIds) {
    MutableGraph mutableGraph = mutGraph().setDirected(true);
    List<MutableNode> linkSources =
        graph.getAdjacencyList()
            .getGraphVertexMap()
            .entrySet()
            .stream()
            .filter(entry -> nodeIds.contains(entry.getKey()))
            .map(entry
                -> mutNode(entry.getValue().getName()).attrs().add(new MapAttributes<>().add("ID", entry.getKey())))
            .collect(Collectors.toList());
    mutableGraph.add(linkSources);

    graph.getAdjacencyList()
        .getAdjacencyMap()
        .entrySet()
        .stream()
        .filter(entry -> nodeIds.contains(entry.getKey()))
        .forEach(entry -> {
          if (!entry.getValue().getEdges().isEmpty()) {
            MutableGraph cluster = mutGraph().setCluster(true).setName(entry.getKey()).setStrict(true);
            cluster.add(generateGraph(graph, entry.getValue().getEdges()));
            mutableGraph.add(cluster);
          }
          if (!entry.getValue().getNextIds().isEmpty()) {
            mutableGraph.add(generateGraph(graph, Collections.singletonList(entry.getValue().getNextIds().get(0))));
          }
        });

    return mutableGraph;
  }

  private void addLinksToGraph(MutableGraph mutableGraph, OrchestrationGraphDTO orchestrationGraph) {
    Map<String, GraphVertex> graphVertexMap = orchestrationGraph.getAdjacencyList().getGraphVertexMap();
    Set<MutableNode> nodes = (Set<MutableNode>) mutableGraph.nodes();
    nodes.forEach(node -> {
      EdgeList edgeList = orchestrationGraph.getAdjacencyList().getAdjacencyMap().get(node.attrs().get("ID"));
      if (!edgeList.getEdges().isEmpty() && !edgeList.getNextIds().isEmpty()) {
        node.addLink(edgeList.getEdges().stream().map(s -> graphVertexMap.get(s).getName()).toArray(String[] ::new));
        node.addLink(graphVertexMap.get(edgeList.getNextIds().get(0)).getName());
      } else if (!edgeList.getEdges().isEmpty()) {
        node.addLink(edgeList.getEdges().stream().map(s -> graphVertexMap.get(s).getName()).toArray(String[] ::new));
      } else if (!edgeList.getNextIds().isEmpty()) {
        node.addLink(graphVertexMap.get(edgeList.getNextIds().get(0)).getName());
      }
    });
  }

  public void breadthFirstTraversal(OrchestrationGraphDTO graph) {
    breadthFirstTraversalInternal(graph.getRootNodeIds().get(0), graph.getAdjacencyList());
  }

  private void breadthFirstTraversalInternal(String nodeId, OrchestrationAdjacencyList adjacencyList) {
    if (adjacencyList.getGraphVertexMap().get(nodeId) == null) {
      return;
    }

    LinkedList<GraphVertex> queue = new LinkedList<>();
    queue.add(adjacencyList.getGraphVertexMap().get(nodeId));

    Set<GraphVertex> visited = new HashSet<>();

    while (!queue.isEmpty()) {
      GraphVertex graphVertex = queue.removeFirst();

      if (visited.contains(graphVertex)) {
        continue;
      }

      visited.add(graphVertex);
      logger.info(graphVertex.getName() + " ");

      List<String> childIds = adjacencyList.getAdjacencyMap().get(graphVertex.getUuid()).getEdges();
      String nextId = adjacencyList.getAdjacencyMap().get(graphVertex.getUuid()).getNextIds().get(0);
      if (childIds.isEmpty() && nextId == null) {
        continue;
      }

      for (String child : childIds) {
        GraphVertex nextVertex = adjacencyList.getGraphVertexMap().get(child);
        if (!visited.contains(nextVertex)) {
          queue.add(nextVertex);
        }
      }

      if (nextId != null) {
        GraphVertex nextVertex = adjacencyList.getGraphVertexMap().get(nextId);
        if (!visited.contains(nextVertex)) {
          queue.add(nextVertex);
        }
      }
    }
  }

  public void depthFirstTraversal(OrchestrationGraphDTO graph) {
    depthFirstTraversalInternal(graph.getRootNodeIds().get(0), graph.getAdjacencyList(), new HashSet<>());
  }

  private void depthFirstTraversalInternal(
      String nodeId, OrchestrationAdjacencyList adjacencyList, Set<String> visited) {
    GraphVertex graphVertex = adjacencyList.getGraphVertexMap().get(nodeId);
    if (graphVertex == null) {
      return;
    }

    visited.add(graphVertex.getUuid());
    logger.info(graphVertex.getName() + " ");

    EdgeList edgeList = adjacencyList.getAdjacencyMap().get(nodeId);
    for (String child : edgeList.getEdges()) {
      depthFirstTraversalInternal(child, adjacencyList, visited);
    }

    if (!edgeList.getNextIds().isEmpty()) {
      depthFirstTraversalInternal(edgeList.getNextIds().get(0), adjacencyList, visited);
    }
  }

  private Renderer buildImageFromMutableGraph(MutableGraph mutableGraph) {
    return Graphviz.fromGraph(mutableGraph)
        .filter(new RoughFilter()
                    .bowing(2)
                    .curveStepCount(6)
                    .roughness(1)
                    .fillStyle(FillStyle.hachure().width(2).gap(5).angle(0))
                    .font("Handlee", "Comic Sans MS"))
        .render(Format.PNG);
  }
}
