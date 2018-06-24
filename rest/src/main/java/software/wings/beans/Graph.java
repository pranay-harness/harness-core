/**
 *
 */

package software.wings.beans;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static software.wings.beans.GraphLink.Builder.aLink;
import static software.wings.sm.TransitionType.SUCCESS;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.data.structure.UUIDGenerator;
import org.mongodb.morphia.annotations.Transient;
import software.wings.common.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Class Graph.
 *
 * @author Rishi
 */
public class Graph {
  private String graphName = Constants.DEFAULT_WORKFLOW_NAME;
  private List<GraphNode> nodes = new ArrayList<>();
  private List<GraphLink> links = new ArrayList<>();
  private Map<String, Graph> subworkflows = new HashMap<>();

  @Transient private Optional<GraphNode> originState;

  @Transient @JsonIgnore private int version;

  /**
   * Gets graph name.
   *
   * @return the graph name
   */
  public String getGraphName() {
    return graphName;
  }

  /**
   * Sets graph name.
   *
   * @param graphName the graph name
   */
  public void setGraphName(String graphName) {
    this.graphName = graphName;
  }

  /**
   * Gets nodes.
   *
   * @return the nodes
   */
  public List<GraphNode> getNodes() {
    return nodes;
  }

  /**
   * Sets nodes.
   *
   * @param nodes the nodes
   */
  public void setNodes(List<GraphNode> nodes) {
    this.nodes = nodes;
  }

  /**
   * Add node.
   *
   * @param node the node
   */
  public void addNode(GraphNode node) {
    this.nodes.add(node);
  }

  /**
   * Gets links.
   *
   * @return the links
   */
  public List<GraphLink> getLinks() {
    return links;
  }

  /**
   * Sets links.
   *
   * @param links the links
   */
  public void setLinks(List<GraphLink> links) {
    this.links = links;
  }

  /**
   * Add link.
   *
   * @param link the link
   */
  public void addLink(GraphLink link) {
    this.links.add(link);
  }

  /**
   * Gets nodes map.
   *
   * @return the nodes map
   */
  @JsonIgnore
  public Map<String, GraphNode> getNodesMap() {
    return getNodes().stream().collect(toMap(GraphNode::getId, identity()));
  }

  /**
   * Getter for property 'version'.
   *
   * @return Value for property 'version'.
   */
  @JsonProperty
  public int getVersion() {
    return version;
  }

  /**
   * Setter for property 'version'.
   *
   * @param version Value to set for property 'version'.
   */
  @JsonIgnore
  public void setVersion(int version) {
    this.version = version;
  }

  public Map<String, Graph> getSubworkflows() {
    return subworkflows;
  }

  public void setSubworkflows(Map<String, Graph> subworkflows) {
    this.subworkflows = subworkflows;
  }

  /**
   * Is linear boolean.
   *
   * @return the boolean
   */
  @JsonIgnore
  public boolean isLinear() {
    if (getNodes() != null && getLinks() != null) {
      Optional<GraphNode> originState = getOriginNode();
      if (originState.isPresent()) {
        List<GraphNode> visitedNodes = newArrayList(getLinearGraphIterator());
        return visitedNodes.containsAll(getNodes());
      }
    }
    return false;
  }

  /**
   * Gets linear graph iterator.
   *
   * @return the linear graph iterator
   */
  @JsonIgnore
  public Iterator<GraphNode> getLinearGraphIterator() {
    Optional<GraphNode> originNode = getOriginNode();
    Map<String, GraphNode> nodesMap = getNodesMap();
    Map<String, GraphLink> linkMap = null;

    linkMap = getLinks().stream().collect(toMap(GraphLink::getFrom, identity()));

    Map<String, GraphLink> finalLinkMap = linkMap;
    return new Iterator<GraphNode>() {
      private GraphNode node = originNode.get();

      @Override
      public boolean hasNext() {
        return node != null;
      }

      @Override
      public GraphNode next() {
        GraphNode currentNode = node;
        node = null;
        GraphLink link = finalLinkMap.get(currentNode.getId());
        if (link != null) {
          node = nodesMap.get(link.getTo());
        }
        return currentNode;
      }
    };
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((graphName == null) ? 0 : graphName.hashCode());
    result = prime * result + ((links == null) ? 0 : links.hashCode());
    result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Graph graph = (Graph) o;

    if (version != graph.version) {
      return false;
    }
    if (graphName != null ? !graphName.equals(graph.graphName) : graph.graphName != null) {
      return false;
    }
    if (nodes != null ? !nodes.equals(graph.nodes) : graph.nodes != null) {
      return false;
    }
    if (links != null ? !links.equals(graph.links) : graph.links != null) {
      return false;
    }
    if (subworkflows != null ? !subworkflows.equals(graph.subworkflows) : graph.subworkflows != null) {
      return false;
    }
    return originState != null ? originState.equals(graph.originState) : graph.originState == null;
  }

  private Optional<GraphNode> getOriginNode() {
    if (originState == null || !originState.isPresent()) {
      originState = getNodes().stream().filter(GraphNode::isOrigin).findFirst();
    }
    return originState;
  }

  @Override
  public String toString() {
    return "Graph{"
        + "graphName='" + graphName + '\'' + ", nodes=" + nodes + ", links=" + links + ", subworkflows=" + subworkflows
        + ", originState=" + originState + ", version=" + version + '}';
  }

  /**
   * The enum Node ops.
   */
  public enum NodeOps {
    /**
     * Expand node ops.
     */
    EXPAND,
    /**
     * Collapse node ops.
     */
    COLLAPSE
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private String graphName = Constants.DEFAULT_WORKFLOW_NAME;
    private List<GraphNode> nodes = new ArrayList<>();
    private List<GraphLink> links = new ArrayList<>();
    private Map<String, Graph> subworkflows = new HashMap<>();

    private Builder() {}

    /**
     * A graph.
     *
     * @return the builder
     */
    public static Builder aGraph() {
      return new Builder();
    }

    /**
     * With graph name.
     *
     * @param graphName the graph name
     * @return the builder
     */
    public Builder withGraphName(String graphName) {
      this.graphName = graphName;
      return this;
    }

    /**
     * Adds the nodes.
     *
     * @param nodes the nodes
     * @return the builder
     */
    public Builder addNodes(GraphNode... nodes) {
      this.nodes.addAll(asList(nodes));
      return this;
    }

    /**
     * With nodes.
     *
     * @param nodes the nodes
     * @return the builder
     */
    public Builder withNodes(List<GraphNode> nodes) {
      this.nodes = nodes;
      return this;
    }

    /**
     * Adds the subworkflow.
     *
     * @param subworkflows subworkflows
     * @return the builder
     */
    public Builder addSubworkflows(Map<String, Graph> subworkflows) {
      this.subworkflows.putAll(subworkflows);
      return this;
    }

    /**
     * Adds the subworkflow.
     *
     * @param subworkflowName the subworkflowName
     * @param subworkflow the subworkflow
     * @return the builder
     */
    public Builder addSubworkflow(String subworkflowName, Graph subworkflow) {
      this.subworkflows.put(subworkflowName, subworkflow);
      return this;
    }

    /**
     * Adds the links.
     *
     * @param links the links
     * @return the builder
     */
    public Builder addLinks(GraphLink... links) {
      this.links.addAll(asList(links));
      return this;
    }

    /**
     * With links.
     *
     * @param links the links
     * @return the builder
     */
    public Builder withLinks(List<GraphLink> links) {
      this.links = links;
      return this;
    }

    /**
     * But.
     *
     * @return the builder
     */
    public Builder but() {
      return aGraph().withGraphName(graphName).withNodes(nodes).withLinks(links);
    }

    /**
     * Builds the.
     *
     * @return the graph
     */
    public Graph build() {
      Graph graph = new Graph();
      graph.setGraphName(graphName);
      graph.setNodes(nodes);
      graph.setLinks(links);
      graph.setSubworkflows(subworkflows);
      return graph;
    }

    /**
     * Build pipeline graph.
     *
     * @return the graph
     */
    public Graph buildPipeline() {
      Graph graph = new Graph();
      graph.setGraphName(graphName);
      graph.setNodes(nodes);
      links.clear();
      for (int i = 0; i < nodes.size() - 1; i++) {
        links.add(aLink()
                      .withFrom(nodes.get(i).getId())
                      .withTo(nodes.get(i + 1).getId())
                      .withType(SUCCESS.name())
                      .withId(UUIDGenerator.graphIdGenerator("link"))
                      .build());
      }
      graph.setLinks(links);
      return graph;
    }
  }
}
