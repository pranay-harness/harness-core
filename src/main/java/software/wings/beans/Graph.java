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
import software.wings.sm.TransitionType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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

  static final int DEFAULT_INITIAL_X = 50;

  static final int DEFAULT_INITIAL_Y = 80;

  static final int DEFAULT_NODE_WIDTH = 200;

  static final int DEFAULT_NODE_HEIGHT = 100;

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
      if (TransitionType.SUCCESS.name().toLowerCase().equals(link.getType())
          || TransitionType.FAILURE.name().toLowerCase().equals(link.getType())) {
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

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
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
          + ", properties=" + properties + "]";
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
       * @param type the status
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
       * Adds the property.
       *
       * @param name the name
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

  public void repaint(String originNodeId) {
    Map<String, Node> nodesMap = getNodesMap();
    Map<String, List<Link>> repeatLinkMap = getRepeatLinkMap();
    Map<String, Link> nextLinkMap = getNextLinkMap();

    repaint(nodesMap.get(originNodeId), new Area(DEFAULT_INITIAL_X, DEFAULT_INITIAL_Y), nodesMap, nextLinkMap,
        repeatLinkMap);
  }

  private Area repaint(Node node, Area area, Map<String, Node> nodesMap, Map<String, Link> nextLinkMap,
      Map<String, List<Link>> repeatLinkMap) {
    node.setX(area.getX());
    node.setY(area.getY());
    area.setWidth(DEFAULT_NODE_WIDTH);
    area.setHeight(DEFAULT_NODE_HEIGHT);

    // paint the repeat node
    List<Link> repeatLinks = repeatLinkMap.get(node.getId());
    if (repeatLinks != null) {
      Area nodeArea = area;
      for (Link link : repeatLinks) {
        nodeArea = repaint(nodesMap.get(link.getTo()),
            new Area(nodeArea.getX(), nodeArea.getY() + nodeArea.getHeight()), nodesMap, nextLinkMap, repeatLinkMap);
        if (area.getWidth() < nodeArea.getWidth()) {
          area.setWidth(nodeArea.getWidth());
        }
        area.setHeight(nodeArea.getHeight() + DEFAULT_NODE_HEIGHT);
      }
    }

    if (nextLinkMap.get(node.getId()) != null) {
      Area nodeArea = repaint(nodesMap.get(nextLinkMap.get(node.getId()).getTo()),
          new Area(area.getX() + area.getWidth(), area.getY()), nodesMap, nextLinkMap, repeatLinkMap);
      if (area.getHeight() < nodeArea.getHeight()) {
        area.setHeight(nodeArea.getHeight());
      }
      area.setWidth(nodeArea.getWidth() + DEFAULT_NODE_WIDTH);
    }
    return area;
  }

  static class Area {
    int x;
    int y;
    int width;
    int height;

    public Area(int x, int y) {
      super();
      this.x = x;
      this.y = y;
    }

    public Area(int x, int y, int width, int height) {
      super();
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
    }

    public int getX() {
      return x;
    }

    public void setX(int x) {
      this.x = x;
    }

    public int getY() {
      return y;
    }

    public void setY(int y) {
      this.y = y;
    }

    public int getWidth() {
      return width;
    }

    public void setWidth(int width) {
      this.width = width;
    }

    public int getHeight() {
      return height;
    }

    public void setHeight(int height) {
      this.height = height;
    }
  }
}
