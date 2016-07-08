/**
 *
 */

package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * The Class Orchestration.
 *
 * @author Rishi
 */
@Entity(value = "orchestrations", noClassnameStored = true)
public class Orchestration extends Workflow {
  @Indexed @Reference(idOnly = true) @NotNull private Environment environment;

  private WorkflowType workflowType;

  /**
   * Gets environment.
   *
   * @return the environment
   */
  public Environment getEnvironment() {
    return environment;
  }

  /**
   * Sets environment.
   *
   * @param environment the environment
   */
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  /**
   * Gets workflow type.
   *
   * @return the workflow type
   */
  public WorkflowType getWorkflowType() {
    return workflowType;
  }

  /**
   * Sets workflow type.
   *
   * @param workflowType the workflow type
   */
  public void setWorkflowType(WorkflowType workflowType) {
    this.workflowType = workflowType;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String description;
    private List<Service> services = new ArrayList<>();
    private Graph graph;
    private Environment environment;
    private WorkflowType workflowType;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * An orchestration builder.
     *
     * @return the builder
     */
    public static Builder anOrchestration() {
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
    public Builder withServices(List<Service> services) {
      this.services = services;
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
     * With environment builder.
     *
     * @param environment the environment
     * @return the builder
     */
    public Builder withEnvironment(Environment environment) {
      this.environment = environment;
      return this;
    }

    /**
     * With workflow type builder.
     *
     * @param workflowType the workflow type
     * @return the builder
     */
    public Builder withWorkflowType(WorkflowType workflowType) {
      this.workflowType = workflowType;
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
      return anOrchestration()
          .withName(name)
          .withDescription(description)
          .withGraph(graph)
          .withEnvironment(environment)
          .withWorkflowType(workflowType)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build orchestration.
     *
     * @return the orchestration
     */
    public Orchestration build() {
      Orchestration orchestration = new Orchestration();
      orchestration.setName(name);
      orchestration.setDescription(description);
      orchestration.setServices(services);
      orchestration.setGraph(graph);
      orchestration.setEnvironment(environment);
      orchestration.setWorkflowType(workflowType);
      orchestration.setUuid(uuid);
      orchestration.setAppId(appId);
      orchestration.setCreatedBy(createdBy);
      orchestration.setCreatedAt(createdAt);
      orchestration.setLastUpdatedBy(lastUpdatedBy);
      orchestration.setLastUpdatedAt(lastUpdatedAt);
      orchestration.setActive(active);
      return orchestration;
    }
  }
}
