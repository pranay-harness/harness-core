/**
 *
 */

package software.wings.beans;

import com.google.common.collect.Maps;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Class Orchestration.
 *
 * @author Rishi
 */
@Entity(value = "orchestrations", noClassnameStored = true)
public class Orchestration extends Workflow {
  private WorkflowType workflowType;

  @Embedded private Map<String, EntityVersion> envIdVersionMap = Maps.newHashMap();

  private boolean targetToAllEnv;

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

  public Map<String, EntityVersion> getEnvIdVersionMap() {
    return envIdVersionMap;
  }

  public void setEnvIdVersionMap(Map<String, EntityVersion> envIdVersionMap) {
    this.envIdVersionMap = envIdVersionMap;
  }

  public boolean getTargetToAllEnv() {
    return targetToAllEnv;
  }

  public void setTargetToAllEnv(boolean targetToAllEnv) {
    this.targetToAllEnv = targetToAllEnv;
  }

  public static final class OrchestrationBuilder {
    private WorkflowType workflowType;
    private Integer defaultVersion;
    private Map<String, EntityVersion> envIdVersionMap = Maps.newHashMap();
    private boolean targetToAllEnv;
    private String name;
    private String description;
    private List<Service> services = new ArrayList<>();
    private Graph graph;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private OrchestrationBuilder() {}

    public static OrchestrationBuilder anOrchestration() {
      return new OrchestrationBuilder();
    }

    public OrchestrationBuilder withWorkflowType(WorkflowType workflowType) {
      this.workflowType = workflowType;
      return this;
    }

    public OrchestrationBuilder withDefaultVersion(Integer defaultVersion) {
      this.defaultVersion = defaultVersion;
      return this;
    }

    public OrchestrationBuilder withEnvIdVersionMap(Map<String, EntityVersion> envIdVersionMap) {
      this.envIdVersionMap = envIdVersionMap;
      return this;
    }

    public OrchestrationBuilder withTargetToAllEnv(boolean targetToAllEnv) {
      this.targetToAllEnv = targetToAllEnv;
      return this;
    }

    public OrchestrationBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public OrchestrationBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public OrchestrationBuilder withServices(List<Service> services) {
      this.services = services;
      return this;
    }

    public OrchestrationBuilder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public OrchestrationBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public OrchestrationBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public OrchestrationBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public OrchestrationBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public OrchestrationBuilder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public OrchestrationBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Orchestration build() {
      Orchestration orchestration = new Orchestration();
      orchestration.setWorkflowType(workflowType);
      orchestration.setDefaultVersion(defaultVersion);
      orchestration.setEnvIdVersionMap(envIdVersionMap);
      orchestration.setTargetToAllEnv(targetToAllEnv);
      orchestration.setName(name);
      orchestration.setDescription(description);
      orchestration.setServices(services);
      orchestration.setGraph(graph);
      orchestration.setUuid(uuid);
      orchestration.setAppId(appId);
      orchestration.setCreatedBy(createdBy);
      orchestration.setCreatedAt(createdAt);
      orchestration.setLastUpdatedBy(lastUpdatedBy);
      orchestration.setLastUpdatedAt(lastUpdatedAt);
      return orchestration;
    }
  }
}
