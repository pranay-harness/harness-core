/**
 *
 */

package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;

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

  public Environment getEnvironment() {
    return environment;
  }

  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  public WorkflowType getWorkflowType() {
    return workflowType;
  }

  public void setWorkflowType(WorkflowType workflowType) {
    this.workflowType = workflowType;
  }

  public static final class Builder {
    private String name;
    private String description;
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

    public static Builder anOrchestration() {
      return new Builder();
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public Builder withEnvironment(Environment environment) {
      this.environment = environment;
      return this;
    }

    public Builder withWorkflowType(WorkflowType workflowType) {
      this.workflowType = workflowType;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

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

    public Orchestration build() {
      Orchestration orchestration = new Orchestration();
      orchestration.setName(name);
      orchestration.setDescription(description);
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
