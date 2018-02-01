/**
 *
 */

package software.wings.beans;

import static com.google.common.collect.Lists.newArrayList;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.sm.TransitionType.SUCCESS;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Transient;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.sm.InstanceStatusSummary;

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
  @Embedded private List<Node> nodes = new ArrayList<>();
  private List<Link> links = new ArrayList<>();
  @Embedded private Map<String, Graph> subworkflows = new HashMap<>();

  @Transient private Optional<Node> originState;

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
   * Add node.
   *
   * @param node the node
   */
  public void addNode(Node node) {
    this.nodes.add(node);
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
   * Add link.
   *
   * @param link the link
   */
  public void addLink(Link link) {
    this.links.add(link);
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
      throw e;
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

  private Optional<Node> getOriginNode() {
    if (originState == null || !originState.isPresent()) {
      originState = getNodes().stream().filter(Node::isOrigin).findFirst();
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
   * The Class Node.
   */
  public static class Group extends Node {
    private List<Node> elements = new ArrayList<>();

    private ExecutionStrategy executionStrategy = ExecutionStrategy.PARALLEL;

    /**
     * Instantiates a new Group.
     */
    public Group() {
      setType("GROUP");
    }

    /**
     * Gets elements.
     *
     * @return the elements
     */
    public List<Node> getElements() {
      return elements;
    }

    /**
     * Sets elements.
     *
     * @param elements the elements
     */
    public void setElements(List<Node> elements) {
      this.elements = elements;
    }

    /**
     * Gets execution strategy.
     *
     * @return the execution strategy
     */
    public ExecutionStrategy getExecutionStrategy() {
      return executionStrategy;
    }

    /**
     * Sets execution strategy.
     *
     * @param executionStrategy the execution strategy
     */
    public void setExecutionStrategy(ExecutionStrategy executionStrategy) {
      this.executionStrategy = executionStrategy;
    }
  }

  /**
   * The Class Node.
   */
  public static class Node {
    private String id = UUIDGenerator.getUuid();
    private String name;
    private String type;
    private boolean rollback;
    private String status;
    private int x;
    private int y;
    private int width;
    private int height;

    private Object executionSummary;
    private Object executionDetails;
    private String detailsReference;
    private boolean origin;

    private boolean valid = true;
    private String validationMessage;
    private Map<String, String> inValidFieldMessages;

    private List<ElementExecutionSummary> elementStatusSummary;
    private List<InstanceStatusSummary> instanceStatusSummary;
    private List<TemplateExpression> templateExpressions;
    private List<NameValuePair> variableOverrides;

    @Embedded private Map<String, Object> properties = new HashMap<>();

    private Node next;
    private Group group;

    public Node clone() {
      Node clonedNode = aNode()
                            .withId("node_" + UUIDGenerator.getUuid())
                            .withName(getName())
                            .withType(getType())
                            .withRollback(getRollback())
                            .withStatus(getStatus())
                            .withX(getX())
                            .withY(getY())
                            .withWidth(getWidth())
                            .withHeight(getHeight())
                            .withTemplateExpressions(getTemplateExpressions())
                            .withValid(isValid())
                            .withValidationMessage(getValidationMessage())
                            .withOrigin(isOrigin())
                            .build();
      clonedNode.setProperties(getProperties());
      return clonedNode;
    }

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

    public boolean getRollback() {
      return rollback;
    }

    public void setRollback(boolean rollback) {
      this.rollback = rollback;
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

    public List<NameValuePair> getVariableOverrides() {
      return variableOverrides;
    }

    public void setVariableOverrides(List<NameValuePair> variableOverrides) {
      this.variableOverrides = variableOverrides;
    }

    /**
     * Gets next.
     *
     * @return the next
     */
    public Node getNext() {
      return next;
    }

    /**
     * Sets next.
     *
     * @param next the next
     */
    public void setNext(Node next) {
      this.next = next;
    }

    /**
     * Gets group.
     *
     * @return the group
     */
    public Group getGroup() {
      return group;
    }

    /**
     * Sets group.
     *
     * @param group the group
     */
    public void setGroup(Group group) {
      this.group = group;
    }

    /**
     * Is origin boolean.
     *
     * @return the boolean
     */
    public boolean isOrigin() {
      return origin;
    }

    /**
     * Sets origin.
     *
     * @param origin the origin
     */
    public void setOrigin(boolean origin) {
      this.origin = origin;
    }

    /**
     * Gets element status summary.
     *
     * @return the element status summary
     */
    public List<ElementExecutionSummary> getElementStatusSummary() {
      return elementStatusSummary;
    }

    /**
     * Sets element status summary.
     *
     * @param elementStatusSummary the element status summary
     */
    public void setElementStatusSummary(List<ElementExecutionSummary> elementStatusSummary) {
      this.elementStatusSummary = elementStatusSummary;
    }

    /**
     * Gets instance status summary.
     *
     * @return the instance status summary
     */
    public List<InstanceStatusSummary> getInstanceStatusSummary() {
      return instanceStatusSummary;
    }

    /**
     * Sets instance status summary.
     *
     * @param instanceStatusSummary the instance status summary
     */
    public void setInstanceStatusSummary(List<InstanceStatusSummary> instanceStatusSummary) {
      this.instanceStatusSummary = instanceStatusSummary;
    }

    /**
     * Gets template expressions
     *
     * @return the template expressions
     */
    public List<TemplateExpression> getTemplateExpressions() {
      return templateExpressions;
    }

    /**
     * Sets template expressions
     * @param templateExpressions
     */
    public void setTemplateExpressions(List<TemplateExpression> templateExpressions) {
      this.templateExpressions = templateExpressions;
    }

    public boolean isValid() {
      return valid;
    }

    public void setValid(boolean valid) {
      this.valid = valid;
    }

    public String getValidationMessage() {
      return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
      this.validationMessage = validationMessage;
    }

    public Map<String, String> getInValidFieldMessages() {
      return inValidFieldMessages;
    }

    public void setInValidFieldMessages(Map<String, String> inValidFieldMessages) {
      this.inValidFieldMessages = inValidFieldMessages;
    }

    public boolean validate() {
      if (isEmpty(inValidFieldMessages)) {
        valid = true;
        validationMessage = null;
      } else {
        valid = false;
        validationMessage = String.format(Constants.STEP_VALIDATION_MESSAGE, inValidFieldMessages.keySet());
      }
      return valid;
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
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      Node other = (Node) obj;
      if (id == null) {
        if (other.id != null) {
          return false;
        }
      } else if (!id.equals(other.id)) {
        return false;
      }
      if (name == null) {
        if (other.name != null) {
          return false;
        }
      } else if (!name.equals(other.name)) {
        return false;
      }
      if (properties == null) {
        if (other.properties != null) {
          return false;
        }
      } else if (!properties.equals(other.properties)) {
        return false;
      }
      if (type == null) {
        if (other.type != null) {
          return false;
        }
      } else if (!type.equals(other.type)) {
        return false;
      }
      if (x != other.x) {
        return false;
      }
      if (y != other.y) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("id", id)
          .add("name", name)
          .add("type", type)
          .add("status", status)
          .add("x", x)
          .add("y", y)
          .add("width", width)
          .add("height", height)
          .add("executionSummary", executionSummary)
          .add("executionDetails", executionDetails)
          .add("detailsReference", detailsReference)
          .add("properties", properties)
          .add("next", next == null ? null : next.getId())
          .add("group", group == null ? null : group.getId())
          .toString();
    }

    /**
     * The Class Builder.
     */
    public static final class Builder {
      private String id = UUIDGenerator.getUuid();
      private String name;
      private String type;
      private String status;
      private int x;
      private int y;
      private int width;
      private int height;
      private boolean rollback;
      private Map<String, Object> properties = new HashMap<>();
      private List<TemplateExpression> templateExpressions;
      private List<NameValuePair> variableOverrides;

      private Object executionSummary;
      private Object executionDetails;
      private String detailsReference;
      private boolean expanded;
      private boolean origin;

      private boolean valid = true;
      private String validationMessage;
      private Map<String, String> inValidFieldMessages;

      private List<ElementExecutionSummary> elementStatusSummary;
      private List<InstanceStatusSummary> instanceStatusSummary;

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
       * @param variableOverrides  the variableOverrides
       * @return the builder
       */
      public Builder withVariableOverrides(List<NameValuePair> variableOverrides) {
        this.variableOverrides = variableOverrides;
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
       * With origin.
       *
       * @param origin the origin
       * @return the builder
       */
      public Builder withOrigin(boolean origin) {
        this.origin = origin;
        return this;
      }

      /**
       * With origin.
       *
       * @param rollback the origin
       * @return the builder
       */
      public Builder withRollback(boolean rollback) {
        this.rollback = rollback;
        return this;
      }

      /**
       * With template expressions
       */
      public Builder withTemplateExpressions(List<TemplateExpression> templateExpressions) {
        this.templateExpressions = templateExpressions;
        return this;
      }

      /**
       * With Execution Summary builder
       */
      public Builder withExecutionSummary(Object executionSummary) {
        this.executionSummary = executionSummary;
        return this;
      }

      /**
       * With Execution Details builder
       */
      public Builder withExecutionDetails(Object executionDetails) {
        this.executionDetails = executionDetails;
        return this;
      }

      /**
       * With Details Reference builder
       */
      public Builder withDetailsRefernce(String detailsReference) {
        this.detailsReference = detailsReference;
        return this;
      }

      /**
       * With Expanded Builder
       */
      public Builder withExpanded(boolean expanded) {
        this.expanded = expanded;
        return this;
      }

      /**
       * With Valid Builder
       */
      public Builder withValid(boolean valid) {
        this.valid = valid;
        return this;
      }

      /**
       * With Valid Builder
       */
      public Builder withValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
        return this;
      }

      /**
       * But.
       *
       * @return the builder
       */
      public Builder but() {
        return aNode()
            .withId(id)
            .withName(name)
            .withType(type)
            .withX(x)
            .withY(y)
            .withProperties(properties)
            .withTemplateExpressions(templateExpressions)
            .withVariableOverrides(variableOverrides);
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
        node.setOrigin(origin);
        node.setRollback(rollback);
        node.setTemplateExpressions(templateExpressions);
        node.setVariableOverrides(variableOverrides);
        return node;
      }
    }
  }

  /**
   * The Class Link.
   */
  public static class Link {
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
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      Link other = (Link) obj;
      if (from == null) {
        if (other.from != null) {
          return false;
        }
      } else if (!from.equals(other.from)) {
        return false;
      }
      if (id == null) {
        if (other.id != null) {
          return false;
        }
      } else if (!id.equals(other.id)) {
        return false;
      }
      if (to == null) {
        if (other.to != null) {
          return false;
        }
      } else if (!to.equals(other.to)) {
        return false;
      }
      if (type == null) {
        if (other.type != null) {
          return false;
        }
      } else if (!type.equals(other.type)) {
        return false;
      }
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
    public Builder addNodes(Node... nodes) {
      this.nodes.addAll(asList(nodes));
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
    public Builder addLinks(Link... links) {
      this.links.addAll(asList(links));
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
