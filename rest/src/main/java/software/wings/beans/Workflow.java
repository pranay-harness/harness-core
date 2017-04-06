/**
 *
 */

package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * The Class Workflow.
 *
 * @author Rishi
 */
@Entity(value = "workflows", noClassnameStored = true)
public class Workflow extends Base {
  @NotNull private String name;
  private String description;

  private WorkflowType workflowType;

  private String envId;

  private Integer defaultVersion;

  @Transient private String notes;

  @Transient private OrchestrationWorkflow orchestrationWorkflow;

  @Transient private List<Service> services = new ArrayList<>();
  @Transient private List<WorkflowExecution> workflowExecutions = new ArrayList<>();

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

  public WorkflowType getWorkflowType() {
    return workflowType;
  }

  public void setWorkflowType(WorkflowType workflowType) {
    this.workflowType = workflowType;
  }

  /**
   * Sets description.
   *
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  public OrchestrationWorkflow getOrchestrationWorkflow() {
    return orchestrationWorkflow;
  }

  public void setOrchestrationWorkflow(OrchestrationWorkflow orchestrationWorkflow) {
    this.orchestrationWorkflow = orchestrationWorkflow;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public Integer getDefaultVersion() {
    return defaultVersion;
  }

  public void setDefaultVersion(Integer defaultVersion) {
    this.defaultVersion = defaultVersion;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public List<Service> getServices() {
    return services;
  }

  public void setServices(List<Service> services) {
    this.services = services;
  }

  public List<WorkflowExecution> getWorkflowExecutions() {
    return workflowExecutions;
  }

  public void setWorkflowExecutions(List<WorkflowExecution> workflowExecutions) {
    this.workflowExecutions = workflowExecutions;
  }

  public static final class WorkflowBuilder {
    private String name;
    private String description;
    private WorkflowType workflowType;
    private String envId;
    private Integer defaultVersion;
    private String notes;
    private OrchestrationWorkflow orchestrationWorkflow;
    private List<Service> services;
    private List<WorkflowExecution> workflowExecutions = new ArrayList<>();
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private WorkflowBuilder() {}

    public static WorkflowBuilder aWorkflow() {
      return new WorkflowBuilder();
    }

    public WorkflowBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public WorkflowBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public WorkflowBuilder withWorkflowType(WorkflowType workflowType) {
      this.workflowType = workflowType;
      return this;
    }

    public WorkflowBuilder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public WorkflowBuilder withDefaultVersion(Integer defaultVersion) {
      this.defaultVersion = defaultVersion;
      return this;
    }

    public WorkflowBuilder withNotes(String notes) {
      this.notes = notes;
      return this;
    }

    public WorkflowBuilder withOrchestrationWorkflow(OrchestrationWorkflow orchestrationWorkflow) {
      this.orchestrationWorkflow = orchestrationWorkflow;
      return this;
    }

    public WorkflowBuilder withServices(List<Service> services) {
      this.services = services;
      return this;
    }

    public WorkflowBuilder withWorkflowExecutions(List<WorkflowExecution> workflowExecutions) {
      this.workflowExecutions = workflowExecutions;
      return this;
    }

    public WorkflowBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public WorkflowBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public WorkflowBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public WorkflowBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public WorkflowBuilder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public WorkflowBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Workflow build() {
      Workflow workflow = new Workflow();
      workflow.setName(name);
      workflow.setDescription(description);
      workflow.setWorkflowType(workflowType);
      workflow.setEnvId(envId);
      workflow.setDefaultVersion(defaultVersion);
      workflow.setNotes(notes);
      workflow.setOrchestrationWorkflow(orchestrationWorkflow);
      workflow.setServices(services);
      workflow.setWorkflowExecutions(workflowExecutions);
      workflow.setUuid(uuid);
      workflow.setAppId(appId);
      workflow.setCreatedBy(createdBy);
      workflow.setCreatedAt(createdAt);
      workflow.setLastUpdatedBy(lastUpdatedBy);
      workflow.setLastUpdatedAt(lastUpdatedAt);
      return workflow;
    }
  }
}
