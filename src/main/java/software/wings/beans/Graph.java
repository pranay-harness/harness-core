/**
 *
 */

package software.wings.beans;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.google.common.base.Throwables;

import com.fasterxml.jackson.annotation.JsonIgnore;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.sm.TransitionType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// TODO: Auto-generated Javadoc

/**
 * The Class Graph.
 *
 * @author Rishi
 */
public class Graph {
  /**
   * The constant ORIGIN_STATE.
   */
  public static final String ORIGIN_STATE = "ORIGIN";

  /**
   * The Default initial x.
   */
  static final int DEFAULT_INITIAL_X = 0;

  /**
   * The Default initial y.
   */
  static final int DEFAULT_INITIAL_Y = 0;

  /**
   * The Default node width.
   */
  static final int DEFAULT_NODE_WIDTH = 75;

  /**
   * The Default node height.
   */
  static final int DEFAULT_NODE_HEIGHT = 50;

  /**
   * The Default arrow width.
   */
  static final int DEFAULT_ARROW_WIDTH = 100;

  /**
   * The Default arrow height.
   */
  static final int DEFAULT_ARROW_HEIGHT = 75;

  /**
   * The Default group padding.
   */
  static final int DEFAULT_GROUP_PADDING = 10;

  private String graphName = Constants.DEFAULT_WORKFLOW_NAME;
  private List<Node> nodes = new ArrayList<>();

  private List<Link> links = new ArrayList<>();

  private Optional<Node> originState = null;

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
  public List<Node> getNodes() {
    return nodes;
  }

  /**
   * Sets nodes.
   *
   * @param nodes the nodes
   */
  public void setNodes(List<Node> nodes) {
    this.nodes = nodes;
  }

  /**
   * Gets links.
   *
   * @return the links
   */
  public List<Link> getLinks() {
    return links;
  }

  /**
   * Sets links.
   *
   * @param links the links
   */
  public void setLinks(List<Link> links) {
    this.links = links;
  }

  /**
   * Gets nodes map.
   *
   * @return the nodes map
   */
  @JsonIgnore
  public Map<String, Node> getNodesMap() {
    return getNodes().stream().collect(toMap(Node::getId, identity()));
  }

  /**
   * Gets repeat links map.
   *
   * @return the repeat links map
   */
  @JsonIgnore
  public Map<String, List<Link>> getRepeatLinkMap() {
    Map<String, List<Link>> map = new HashMap<>();
    getLinks().forEach(link -> {
      if (TransitionType.REPEAT.name().toLowerCase().equals(link.getType())) {
        List<Link> toList = map.get(link.getFrom());
        if (toList == null) {
          toList = new ArrayList<>();
          map.put(link.getFrom(), toList);
        }
        toList.add(link);
      }
    });
    return map;
  }

  /**
   * Gets next links map.
   *
   * @return the repeat links map
   */
  @JsonIgnore
  public Map<String, Link> getNextLinkMap() {
    Map<String, Link> map = new HashMap<>();
    getLinks().forEach(link -> {
      if (!"repeat".equals(link.getType())) {
        map.put(link.getFrom(), link);
      }
    });
    return map;
  }

  /**
   * Is linear boolean.
   *
   * @return the boolean
   */
  @JsonIgnore
  public boolean isLinear() {
    if (getNodes() != null && getLinks() != null) {
      Optional<Node> originState = getOriginNode();
      if (originState.isPresent()) {
        List<Node> visitedNodes = newArrayList(getLinearGraphIterator());
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
  public Iterator<Node> getLinearGraphIterator() {
    Optional<Node> originNode = getOriginNode();
    Map<String, Node> nodesMap = getNodesMap();
    Map<String, Link> linkMap = null;

    try {
      linkMap = getLinks().stream().collect(toMap(Link::getFrom, identity()));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }

    Map<String, Link> finalLinkMap = linkMap;
    return new Iterator<Node>() {
      private Node node = originNode.get();

      @Override
      public boolean hasNext() {
        return node != null;
      }

      @Override
      public Node next() {
        Node currentNode = node;
        node = null;
        Link link = finalLinkMap.get(currentNode.getId());
        if (link != null) {
          node = nodesMap.get(link.getTo());
        }
        return currentNode;
      }
    };
  }

  /**
   * Repaint.
   *
   * @param originNodeId the origin node id
   */
  public void repaint(String originNodeId) {
    Map<String, Node> nodesMap = getNodesMap();
    Map<String, List<Link>> repeatLinkMap = getRepeatLinkMap();
    Map<String, Link> nextLinkMap = getNextLinkMap();

    extrapolateDimensions(nodesMap.get(originNodeId), nodesMap, nextLinkMap, repeatLinkMap);
    List<Link> updatedLinks = new ArrayList<>();
    repaint(nodesMap.get(originNodeId), DEFAULT_INITIAL_X, DEFAULT_INITIAL_Y, nodesMap, nextLinkMap, repeatLinkMap,
        updatedLinks);
    setLinks(updatedLinks);
  }

  private void extrapolateDimensions(
      Node node, Map<String, Node> nodesMap, Map<String, Link> nextLinkMap, Map<String, List<Link>> repeatLinkMap) {
    node.setWidth(DEFAULT_NODE_WIDTH);
    node.setHeight(DEFAULT_NODE_HEIGHT);
    List<Link> repeatLinks = repeatLinkMap.get(node.getId());
    if (repeatLinks != null) {
      for (Link link : repeatLinks) {
        Node repeatNode = nodesMap.get(link.getTo());
        extrapolateDimensions(repeatNode, nodesMap, nextLinkMap, repeatLinkMap);
        if (node.getWidth() < repeatNode.getWidth()) {
          node.setWidth(repeatNode.getWidth());
        }
        node.setHeight(node.getHeight() + repeatNode.getHeight() + DEFAULT_ARROW_HEIGHT);
      }
    }
    if (nextLinkMap.get(node.getId()) != null) {
      Node nextNode = nodesMap.get(nextLinkMap.get(node.getId()).getTo());
      extrapolateDimensions(nextNode, nodesMap, nextLinkMap, repeatLinkMap);
      if (node.getHeight() < nextNode.getHeight()) {
        node.setHeight(nextNode.getHeight());
      }
      node.setWidth(node.getWidth() + nextNode.getWidth() + DEFAULT_ARROW_WIDTH);
    }
  }

  private void repaint(Node node, int nodeX, int nodeY, Map<String, Node> nodesMap, Map<String, Link> nextLinkMap,
      Map<String, List<Link>> repeatLinkMap, List<Link> updatedLinks) {
    node.setX(nodeX);
    node.setY(nodeY);

    // repaint the repeat node
    List<Link> repeatLinks = repeatLinkMap.get(node.getId());
    if (repeatLinks != null) {
      int y = nodeY + DEFAULT_NODE_HEIGHT + DEFAULT_ARROW_HEIGHT;

      Node groupNode =
          Node.Builder.aNode()
              .withId(UUIDGenerator.getUuid())
              .withType("group")
              .withX(nodeX - DEFAULT_GROUP_PADDING)
              .withY(y - DEFAULT_GROUP_PADDING)
              .withWidth(DEFAULT_NODE_WIDTH + 2 * DEFAULT_GROUP_PADDING)
              .withHeight(node.getHeight() + 2 * DEFAULT_GROUP_PADDING - DEFAULT_NODE_HEIGHT - DEFAULT_ARROW_HEIGHT)
              .build();

      getNodes().add(0, groupNode);

      updatedLinks.add(Link.Builder.aLink()
                           .withId(repeatLinks.get(0).getId())
                           .withFrom(repeatLinks.get(0).getFrom())
                           .withTo(groupNode.getId())
                           .withType(repeatLinks.get(0).getType())
                           .build());

      boolean serialRepeat = false;
      if (node.getExecutionSummary() != null && node.getExecutionSummary() instanceof LinkedHashMap
          && ((LinkedHashMap) node.getExecutionSummary()).get("executionStrategy") != null
          && ((LinkedHashMap) node.getExecutionSummary())
                 .get("executionStrategy")
                 .equals(ExecutionStrategy.SERIAL.name())) {
        serialRepeat = false;
      }

      Node priorElement = null;

      for (Link link : repeatLinks) {
        Node repeatNode = nodesMap.get(link.getTo());
        repaint(repeatNode, nodeX, y, nodesMap, nextLinkMap, repeatLinkMap, updatedLinks);
        y += repeatNode.getHeight() + DEFAULT_ARROW_HEIGHT;

        // Take care os serial element links
        if (serialRepeat && priorElement != null) {
          updatedLinks.add(Link.Builder.aLink()
                               .withId(UUIDGenerator.getUuid())
                               .withFrom(priorElement.getId())
                               .withTo(repeatNode.getId())
                               .withType(repeatLinks.get(0).getType())
                               .build());
        }
        priorElement = repeatNode;
      }
    }

    if (nextLinkMap.get(node.getId()) != null) {
      Node nextNode = nodesMap.get(nextLinkMap.get(node.getId()).getTo());
      if (repeatLinks == null
          || !(TransitionType.REPEAT.name().toLowerCase().equals(nextNode.getType())
                 || TransitionType.FORK.name().toLowerCase().equals(nextNode.getType()))) {
        repaint(nextNode, nodeX + DEFAULT_NODE_WIDTH + DEFAULT_ARROW_WIDTH, nodeY, nodesMap, nextLinkMap, repeatLinkMap,
            updatedLinks);
      } else {
        repaint(nextNode, nodeX + node.getWidth(), nodeY, nodesMap, nextLinkMap, repeatLinkMap, updatedLinks);
      }
      updatedLinks.add(nextLinkMap.get(node.getId()));
    }
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

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Graph other = (Graph) obj;
    if (graphName == null) {
      if (other.graphName != null)
        return false;
    } else if (!graphName.equals(other.graphName))
      return false;
    if (links == null) {
      if (other.links != null)
        return false;
    } else if (!links.equals(other.links))
      return false;
    if (nodes == null) {
      if (other.nodes != null)
        return false;
    } else if (!nodes.equals(other.nodes))
      return false;
    return true;
  }

  private Optional<Node> getOriginNode() {
    if (originState == null) {
      originState = getNodes().stream().filter(Node::isOrigin).findFirst();
    }
    return originState;
  }

  @Override
  public String toString() {
    return "Graph [graphName=" + graphName + ", nodes=" + nodes + ", links=" + links + ", originState=" + originState
        + "]";
  }

  /**
   * The Class Node.
   */
  public static class Node implements Serializable {
    private static final long serialVersionUID = 7894954599933362678L;

    private String id;
    private String name;
    private String type;
    private String status;
    private int x;
    private int y;
    private int width;
    private int height;

    private Object executionSummary;
    private Object executionDetails;
    private String detailsReference;
    private boolean expanded;

    private Map<String, Object> properties = new HashMap<>();

    /**
     * Gets id.
     *
     * @return the id
     */
    public String getId() {
      return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(String id) {
      this.id = id;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * Gets type.
     *
     * @return the type
     */
    public String getType() {
      return type;
    }

    /**
     * Sets type.
     *
     * @param type the type
     */
    public void setType(String type) {
      this.type = type;
    }

    /**
     * Gets x.
     *
     * @return the x
     */
    public int getX() {
      return x;
    }

    /**
     * Sets x.
     *
     * @param x the x
     */
    public void setX(int x) {
      this.x = x;
    }

    /**
     * Gets y.
     *
     * @return the y
     */
    public int getY() {
      return y;
    }

    /**
     * Sets y.
     *
     * @param y the y
     */
    public void setY(int y) {
      this.y = y;
    }

    /**
     * Gets width.
     *
     * @return the width
     */
    public int getWidth() {
      return width;
    }

    /**
     * Sets width.
     *
     * @param width the width
     */
    public void setWidth(int width) {
      this.width = width;
    }

    /**
     * Gets height.
     *
     * @return the height
     */
    public int getHeight() {
      return height;
    }

    /**
     * Sets height.
     *
     * @param height the height
     */
    public void setHeight(int height) {
      this.height = height;
    }

    /**
     * Gets properties.
     *
     * @return the properties
     */
    public Map<String, Object> getProperties() {
      return properties;
    }

    /**
     * Sets properties.
     *
     * @param properties the properties
     */
    public void setProperties(Map<String, Object> properties) {
      this.properties = properties;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    public String getStatus() {
      return status;
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(String status) {
      this.status = status;
    }

    /**
     * Gets execution summary.
     *
     * @return the execution summary
     */
    public Object getExecutionSummary() {
      return executionSummary;
    }

    /**
     * Sets execution summary.
     *
     * @param executionSummary the execution summary
     */
    public void setExecutionSummary(Object executionSummary) {
      this.executionSummary = executionSummary;
    }

    /**
     * Gets execution details.
     *
     * @return the execution details
     */
    public Object getExecutionDetails() {
      return executionDetails;
    }

    /**
     * Sets execution details.
     *
     * @param executionDetails the execution details
     */
    public void setExecutionDetails(Object executionDetails) {
      this.executionDetails = executionDetails;
    }

    /**
     * Gets details reference.
     *
     * @return the details reference
     */
    public String getDetailsReference() {
      return detailsReference;
    }

    /**
     * Sets details reference.
     *
     * @param detailsReference the details reference
     */
    public void setDetailsReference(String detailsReference) {
      this.detailsReference = detailsReference;
    }

    public boolean isExpanded() {
      return expanded;
    }

    public void setExpanded(boolean expanded) {
      this.expanded = expanded;
    }

    /**
     * Is origin boolean.
     *
     * @return the boolean
     */
    @JsonIgnore
    public boolean isOrigin() {
      return Graph.ORIGIN_STATE.equals(getName()) || Graph.ORIGIN_STATE.equals(getType());
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
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((properties == null) ? 0 : properties.hashCode());
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      result = prime * result + x;
      result = prime * result + y;
      return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Node other = (Node) obj;
      if (id == null) {
        if (other.id != null)
          return false;
      } else if (!id.equals(other.id))
        return false;
      if (name == null) {
        if (other.name != null)
          return false;
      } else if (!name.equals(other.name))
        return false;
      if (properties == null) {
        if (other.properties != null)
          return false;
      } else if (!properties.equals(other.properties))
        return false;
      if (type == null) {
        if (other.type != null)
          return false;
      } else if (!type.equals(other.type))
        return false;
      if (x != other.x)
        return false;
      if (y != other.y)
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "Node [id=" + id + ", name=" + name + ", type=" + type + ", status=" + status + ", x=" + x + ", y=" + y
          + ", width=" + width + ", height=" + height + ", properties=" + properties + "]";
    }

    /**
     * The Class Builder.
     */
    public static final class Builder {
      private String id;
      private String name;
      private String type;
      private String status;
      private int x;
      private int y;
      private int width;
      private int height;
      private Map<String, Object> properties = new HashMap<>();

      private Builder() {}

      /**
       * A node.
       *
       * @return the builder
       */
      public static Builder aNode() {
        return new Builder();
      }

      /**
       * With id.
       *
       * @param id the id
       * @return the builder
       */
      public Builder withId(String id) {
        this.id = id;
        return this;
      }

      /**
       * With name.
       *
       * @param name the name
       * @return the builder
       */
      public Builder withName(String name) {
        this.name = name;
        return this;
      }

      /**
       * With type.
       *
       * @param type the type
       * @return the builder
       */
      public Builder withType(String type) {
        this.type = type;
        return this;
      }

      /**
       * With status.
       *
       * @param status the status
       * @return the builder
       */
      public Builder withStatus(String status) {
        this.status = status;
        return this;
      }

      /**
       * With x.
       *
       * @param x the x
       * @return the builder
       */
      public Builder withX(int x) {
        this.x = x;
        return this;
      }

      /**
       * With y.
       *
       * @param y the y
       * @return the builder
       */
      public Builder withY(int y) {
        this.y = y;
        return this;
      }

      /**
       * With width.
       *
       * @param width the width
       * @return the builder
       */
      public Builder withWidth(int width) {
        this.width = width;
        return this;
      }

      /**
       * With height.
       *
       * @param height the height
       * @return the builder
       */
      public Builder withHeight(int height) {
        this.height = height;
        return this;
      }

      /**
       * Adds the property.
       *
       * @param name  the name
       * @param value the value
       * @return the builder
       */
      public Builder addProperty(String name, Object value) {
        this.properties.put(name, value);
        return this;
      }

      /**
       * With properties.
       *
       * @param properties the properties
       * @return the builder
       */
      public Builder withProperties(Map<String, Object> properties) {
        this.properties = properties;
        return this;
      }

      /**
       * But.
       *
       * @return the builder
       */
      public Builder but() {
        return aNode().withId(id).withName(name).withType(type).withX(x).withY(y).withProperties(properties);
      }

      /**
       * Builds the.
       *
       * @return the node
       */
      public Node build() {
        Node node = new Node();
        node.setId(id);
        node.setName(name);
        node.setType(type);
        node.setStatus(status);
        node.setX(x);
        node.setY(y);
        node.setWidth(width);
        node.setHeight(height);
        node.setProperties(properties);
        return node;
      }
    }
  }

  /**
   * The Class Link.
   */
  public static class Link implements Serializable {
    private static final long serialVersionUID = 7894954599933362678L;

    private String id;
    private String from;
    private String to;
    private String type;

    /**
     * Gets id.
     *
     * @return the id
     */
    public String getId() {
      return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(String id) {
      this.id = id;
    }

    /**
     * Gets from.
     *
     * @return the from
     */
    public String getFrom() {
      return from;
    }

    /**
     * Sets from.
     *
     * @param from the from
     */
    public void setFrom(String from) {
      this.from = from;
    }

    /**
     * Gets to.
     *
     * @return the to
     */
    public String getTo() {
      return to;
    }

    /**
     * Sets to.
     *
     * @param to the to
     */
    public void setTo(String to) {
      this.to = to;
    }

    /**
     * Gets type.
     *
     * @return the type
     */
    public String getType() {
      return type;
    }

    /**
     * Sets type.
     *
     * @param type the type
     */
    public void setType(String type) {
      this.type = type;
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
      result = prime * result + ((from == null) ? 0 : from.hashCode());
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((to == null) ? 0 : to.hashCode());
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Link other = (Link) obj;
      if (from == null) {
        if (other.from != null)
          return false;
      } else if (!from.equals(other.from))
        return false;
      if (id == null) {
        if (other.id != null)
          return false;
      } else if (!id.equals(other.id))
        return false;
      if (to == null) {
        if (other.to != null)
          return false;
      } else if (!to.equals(other.to))
        return false;
      if (type == null) {
        if (other.type != null)
          return false;
      } else if (!type.equals(other.type))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "Link [id=" + id + ", from=" + from + ", to=" + to + ", type=" + type + "]";
    }

    /**
     * The Class Builder.
     */
    public static final class Builder {
      private String id;
      private String from;
      private String to;
      private String type;

      private Builder() {}

      /**
       * A link.
       *
       * @return the builder
       */
      public static Builder aLink() {
        return new Builder();
      }

      /**
       * With id.
       *
       * @param id the id
       * @return the builder
       */
      public Builder withId(String id) {
        this.id = id;
        return this;
      }

      /**
       * With from.
       *
       * @param from the from
       * @return the builder
       */
      public Builder withFrom(String from) {
        this.from = from;
        return this;
      }

      /**
       * With to.
       *
       * @param to the to
       * @return the builder
       */
      public Builder withTo(String to) {
        this.to = to;
        return this;
      }

      /**
       * With type.
       *
       * @param type the type
       * @return the builder
       */
      public Builder withType(String type) {
        this.type = type;
        return this;
      }

      /**
       * But.
       *
       * @return the builder
       */
      public Builder but() {
        return aLink().withId(id).withFrom(from).withTo(to).withType(type);
      }

      /**
       * Builds the.
       *
       * @return the link
       */
      public Link build() {
        Link link = new Link();
        link.setId(id);
        link.setFrom(from);
        link.setTo(to);
        link.setType(type);
        return link;
      }
    }
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private String graphName = Constants.DEFAULT_WORKFLOW_NAME;
    private List<Node> nodes = new ArrayList<>();
    private List<Link> links = new ArrayList<>();

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
    public Builder addNodes(Node... nodes) {
      this.nodes.addAll(Arrays.asList(nodes));
      return this;
    }

    /**
     * With nodes.
     *
     * @param nodes the nodes
     * @return the builder
     */
    public Builder withNodes(List<Node> nodes) {
      this.nodes = nodes;
      return this;
    }

    /**
     * Adds the links.
     *
     * @param links the links
     * @return the builder
     */
    public Builder addLinks(Link... links) {
      this.links.addAll(Arrays.asList(links));
      return this;
    }

    /**
     * With links.
     *
     * @param links the links
     * @return the builder
     */
    public Builder withLinks(List<Link> links) {
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
      return graph;
    }
  }
}
