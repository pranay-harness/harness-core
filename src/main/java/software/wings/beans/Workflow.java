/**
 *
 */

package software.wings.beans;

import org.mongodb.morphia.annotations.Transient;

import javax.validation.constraints.NotNull;

/**
 * The Class Workflow.
 *
 * @author Rishi
 */
public class Workflow extends Base {
  @NotNull private String name;
  private String description;

  @Transient private Graph graph;

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
   * Gets description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets description.
   *
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets graph.
   *
   * @return the graph
   */
  public Graph getGraph() {
    return graph;
  }

  /**
   * Sets graph.
   *
   * @param graph the graph
   */
  public void setGraph(Graph graph) {
    this.graph = graph;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String description;
    private Graph graph;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * A workflow builder.
     *
     * @return the builder
     */
    public static Builder aWorkflow() {
      return new Builder();
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With description builder.
     *
     * @param description the description
     * @return the builder
     */
    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    /**
     * With graph builder.
     *
     * @param graph the graph
     * @return the builder
     */
    public Builder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active builder.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aWorkflow()
          .withName(name)
          .withDescription(description)
          .withGraph(graph)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build workflow.
     *
     * @return the workflow
     */
    public Workflow build() {
      Workflow workflow = new Workflow();
      workflow.setName(name);
      workflow.setDescription(description);
      workflow.setGraph(graph);
      workflow.setUuid(uuid);
      workflow.setAppId(appId);
      workflow.setCreatedBy(createdBy);
      workflow.setCreatedAt(createdAt);
      workflow.setLastUpdatedBy(lastUpdatedBy);
      workflow.setLastUpdatedAt(lastUpdatedAt);
      workflow.setActive(active);
      return workflow;
    }
  }
}
