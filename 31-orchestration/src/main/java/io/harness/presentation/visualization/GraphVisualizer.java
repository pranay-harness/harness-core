package io.harness.presentation.visualization;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.Renderer;
import guru.nidi.graphviz.rough.FillStyle;
import guru.nidi.graphviz.rough.RoughFilter;
import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.presentation.Graph;
import io.harness.presentation.GraphVertex;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
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
}
