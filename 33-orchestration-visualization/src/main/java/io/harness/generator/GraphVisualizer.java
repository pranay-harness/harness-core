package io.harness.generator;

import static guru.nidi.graphviz.attribute.Rank.RankDir.TOP_TO_BOTTOM;
import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;
import static guru.nidi.graphviz.model.Factory.to;

import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.beans.EdgeList;
import io.harness.dto.GraphVertexDTO;
import io.harness.dto.OrchestrationAdjacencyListDTO;
import io.harness.dto.OrchestrationGraphDTO;

import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.MapAttributes;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.Renderer;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import guru.nidi.graphviz.rough.FillStyle;
import guru.nidi.graphviz.rough.RoughFilter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

@ExcludeRedesign
@Slf4j
public class GraphVisualizer {
  private static final String BASE_DIRECTORY = "resources/graphviz/";

  public void generateImage(OrchestrationGraphDTO graph, OutputStream output) throws IOException {
    MutableGraph mutableGraph = generateGraph(graph, graph.getRootNodeIds());
    addLinksToGraph(mutableGraph, graph);
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
    MutableGraph mutableGraph = mutGraph().setDirected(true).setStrict(true).graphAttrs().add(Rank.dir(TOP_TO_BOTTOM));

    graph.getAdjacencyList()
        .getAdjacencyMap()
        .entrySet()
        .stream()
        .filter(entry -> nodeIds.contains(entry.getKey()))
        .forEach(entry -> {
          GraphVertexDTO graphVertex = graph.getAdjacencyList().getGraphVertexMap().get(entry.getKey());
          MutableNode node = mutNode(graphVertex.getName(), true)
                                 .attrs()
                                 .add(new MapAttributes<>().add("ID", entry.getKey()),
                                     Label.of(graphVertex.getName())
                                         .justify(Label.Justification.MIDDLE)
                                         .locate(Label.Location.CENTER));
          if (!entry.getValue().getEdges().isEmpty()) {
            MutableGraph cluster =
                mutGraph().setCluster(true).setName(entry.getKey()).setDirected(true).setStrict(true);
            cluster.add(generateGraph(graph, entry.getValue().getEdges()));
            cluster.graphAttrs().add(Label.of(graphVertex.getMode().name()).justify(Label.Justification.RIGHT));
            cluster.addTo(mutableGraph);
          }
          if (!entry.getValue().getNextIds().isEmpty()) {
            mutableGraph.add(generateGraph(graph, entry.getValue().getNextIds()));
          }
          node.addTo(mutableGraph);
        });

    return mutableGraph;
  }

  private void addLinksToGraph(MutableGraph mutableGraph, OrchestrationGraphDTO orchestrationGraph) {
    Map<String, GraphVertexDTO> graphVertexMap = orchestrationGraph.getAdjacencyList().getGraphVertexMap();
    List<MutableNode> nodes = new ArrayList<>(mutableGraph.nodes());
    nodes.forEach(node -> {
      EdgeList edgeList = orchestrationGraph.getAdjacencyList().getAdjacencyMap().get(node.attrs().get("ID"));
      if (!edgeList.getEdges().isEmpty()) {
        node.addLink(edgeList.getEdges()
                         .stream()
                         .map(s -> graphVertexMap.get(s).getName())
                         .sorted(Comparator.comparing(String::toLowerCase))
                         .toArray(String[] ::new));
      }
      for (String next : edgeList.getNextIds()) {
        node.links().add(node.linkTo(to(mutNode(graphVertexMap.get(next).getName())).with(Label.of("next"))));
      }
    });
  }

  public void breadthFirstTraversal(OrchestrationGraphDTO graph) {
    breadthFirstTraversalInternal(graph.getRootNodeIds().get(0), graph.getAdjacencyList());
  }

  private void breadthFirstTraversalInternal(String nodeId, OrchestrationAdjacencyListDTO adjacencyList) {
    if (adjacencyList.getGraphVertexMap().get(nodeId) == null) {
      return;
    }

    LinkedList<GraphVertexDTO> queue = new LinkedList<>();
    queue.add(adjacencyList.getGraphVertexMap().get(nodeId));

    Set<GraphVertexDTO> visited = new HashSet<>();

    while (!queue.isEmpty()) {
      GraphVertexDTO graphVertex = queue.removeFirst();

      if (visited.contains(graphVertex)) {
        continue;
      }

      visited.add(graphVertex);
      log.info(graphVertex.getName() + " ");

      List<String> childIds = adjacencyList.getAdjacencyMap().get(graphVertex.getUuid()).getEdges();
      String nextId = adjacencyList.getAdjacencyMap().get(graphVertex.getUuid()).getNextIds().get(0);
      if (childIds.isEmpty() && nextId == null) {
        continue;
      }

      for (String child : childIds) {
        GraphVertexDTO nextVertex = adjacencyList.getGraphVertexMap().get(child);
        if (!visited.contains(nextVertex)) {
          queue.add(nextVertex);
        }
      }

      if (nextId != null) {
        GraphVertexDTO nextVertex = adjacencyList.getGraphVertexMap().get(nextId);
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
      String nodeId, OrchestrationAdjacencyListDTO adjacencyList, Set<String> visited) {
    GraphVertexDTO graphVertex = adjacencyList.getGraphVertexMap().get(nodeId);
    if (graphVertex == null) {
      return;
    }

    visited.add(graphVertex.getUuid());
    log.info(graphVertex.getName() + " ");

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
