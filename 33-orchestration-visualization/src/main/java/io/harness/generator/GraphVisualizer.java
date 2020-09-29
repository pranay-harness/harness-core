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
import io.harness.beans.Graph;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.dto.OrchestrationGraph;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
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
  private static final String CLUSTER = "cluster_";
  private static final String SPACE = " ";
  private static final String QUOTE_SIGN = "\"";
  private static final String BASE_DIRECTORY = "resources/graphviz/";

  private final StringWriter dotGraph = new StringWriter(100);

  public void generateImage(Graph graph, OutputStream output) throws IOException {
    generate(graph);
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      logger.info(dotGraph.toString());
      buildImage().toOutputStream(os);
      output.write(os.toByteArray(), 0, os.size());
      output.flush();
    } finally {
      dotGraph.close();
    }
  }

  public void generateImage(Graph graph, String filename) throws IOException {
    generate(graph);
    try {
      logger.info(dotGraph.toString());
      buildImage().toFile(new File(BASE_DIRECTORY, FilenameUtils.getName(filename)));
    } finally {
      dotGraph.close();
    }
  }

  public void generateImage(OrchestrationGraph graph, String filename) throws IOException {
    MutableGraph mutableGraph = generateGraph(graph, graph.getRootNodeIds());
    addLinksToGraph(mutableGraph, graph);

    try {
      buildImageFromMutableGraph(mutableGraph).toFile(new File(BASE_DIRECTORY, FilenameUtils.getName(filename)));
    } finally {
      dotGraph.close();
    }
  }

  private MutableGraph generateGraph(OrchestrationGraph graph, List<String> nodeIds) {
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
        .getAdjacencyList()
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

  private void addLinksToGraph(MutableGraph mutableGraph, OrchestrationGraph orchestrationGraph) {
    Map<String, GraphVertex> graphVertexMap = orchestrationGraph.getAdjacencyList().getGraphVertexMap();
    Set<MutableNode> nodes = (Set<MutableNode>) mutableGraph.nodes();
    nodes.forEach(node -> {
      EdgeList edgeList = orchestrationGraph.getAdjacencyList().getAdjacencyList().get(node.attrs().get("ID"));
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

  public void breadthFirstTraversal(OrchestrationGraph graph) {
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

      List<String> childIds = adjacencyList.getAdjacencyList().get(graphVertex.getUuid()).getEdges();
      String nextId = adjacencyList.getAdjacencyList().get(graphVertex.getUuid()).getNextIds().get(0);
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

  public void depthFirstTraversal(OrchestrationGraph graph) {
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

    EdgeList edgeList = adjacencyList.getAdjacencyList().get(nodeId);
    for (String child : edgeList.getEdges()) {
      depthFirstTraversalInternal(child, adjacencyList, visited);
    }

    if (!edgeList.getNextIds().isEmpty()) {
      depthFirstTraversalInternal(edgeList.getNextIds().get(0), adjacencyList, visited);
    }
  }

  private void generate(Graph graph) {
    dotGraph.append("strict digraph {")
        .append(SPACE)
        .append("compound = true")
        .append(SPACE)
        .append("labeljust = r")
        .append(SPACE);
    GraphVertex current = graph.getGraphVertex();
    while (current != null) {
      shouldGenerateSubgraphStructure(current);
      current = current.getNext();
    }
    shouldGenerateConnections(graph.getGraphVertex());
    dotGraph.append('}');
  }

  /**
   * This method is used to generate nodes in their subgraph clusters
   * @param graphVertex -
   */
  private void shouldGenerateSubgraphStructure(GraphVertex graphVertex) {
    if (graphVertex.getSubgraph() == null) {
      return;
    }

    dotGraph.append("subgraph")
        .append(SPACE)
        .append(obtainClusterName(graphVertex))
        .append('{')
        .append(SPACE)
        .append("label=")
        .append(QUOTE_SIGN)
        .append(graphVertex.getSubgraph().getMode().name())
        .append(QUOTE_SIGN)
        .append(SPACE);

    List<String> vertices = new ArrayList<>();
    // creating subgraph for evert vertex
    for (GraphVertex vertex : graphVertex.getSubgraph().getVertices()) {
      GraphVertex currentVertex = vertex;
      // also we need to check if the subgraph's vertex has subgraph inside or
      // reference to the next node which could potentially contain subgraph
      while (currentVertex != null) {
        vertices.add(currentVertex.getName());
        shouldGenerateSubgraphStructure(currentVertex);
        currentVertex = currentVertex.getNext();
      }
    }
    dotGraph.append(QUOTE_SIGN)
        .append(String.join(QUOTE_SIGN + SPACE + QUOTE_SIGN, vertices))
        .append(QUOTE_SIGN)
        .append('}');
  }

  /**
   * This is used to generate connection between existing vertices
   * @param graphVertex -
   */
  private void shouldGenerateConnections(GraphVertex graphVertex) {
    if (graphVertex == null) {
      return;
    }
    if (graphVertex.getNext() == null && graphVertex.getSubgraph() == null) {
      dotGraph.append(QUOTE_SIGN).append(graphVertex.getName()).append(QUOTE_SIGN);
      return;
    }

    // create links for every subgraph vertex
    if (graphVertex.getSubgraph() != null) {
      dotGraph.append(createLink(graphVertex.getName(),
          graphVertex.getSubgraph()
              .getVertices()
              .stream()
              .map(GraphVertex::getName)
              .collect(Collectors.joining(QUOTE_SIGN + SPACE + QUOTE_SIGN))));
      for (GraphVertex vertex : graphVertex.getSubgraph().getVertices()) {
        shouldGenerateConnections(vertex);
      }
    }

    if (graphVertex.getNext() != null) {
      // if subgraph is not empty that means we have a parent node, so the arrow to the next node should
      // start from the cluster (this vertex subgraph) and not from this vertex
      if (graphVertex.getSubgraph() != null) {
        dotGraph
            .append(createLink(
                graphVertex.getSubgraph().getVertices().stream().map(GraphVertex::getName).findAny().orElse(null),
                graphVertex.getNext().getName()))
            .append("[ltail=")
            .append(obtainClusterName(graphVertex))
            .append(']');
      } else {
        dotGraph.append(createLink(graphVertex.getName(), graphVertex.getNext().getName()));
      }
      shouldGenerateConnections(graphVertex.getNext());
    }
  }

  private String obtainClusterName(GraphVertex vertex) {
    return QUOTE_SIGN + CLUSTER + vertex.getStepType() + vertex.getUuid() + QUOTE_SIGN;
  }

  private StringBuilder createLink(String source, String target) {
    StringBuilder stringBuilder = new StringBuilder();
    return stringBuilder.append(QUOTE_SIGN)
        .append(source)
        .append(QUOTE_SIGN)
        .append("->")
        .append('{')
        .append(QUOTE_SIGN)
        .append(target)
        .append(QUOTE_SIGN)
        .append('}');
  }

  private Renderer buildImage() {
    return Graphviz.fromString(dotGraph.toString())
        .filter(new RoughFilter()
                    .bowing(2)
                    .curveStepCount(6)
                    .roughness(1)
                    .fillStyle(FillStyle.hachure().width(2).gap(5).angle(0))
                    .font("Handlee", "Comic Sans MS"))
        .render(Format.PNG);
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
