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

  public static final class Builder {
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

    private Builder() {}

    public static Builder anOrchestration() {
      return new Builder();
    }

    public Builder withWorkflowType(WorkflowType workflowType) {
      this.workflowType = workflowType;
      return this;
    }

    public Builder withDefaultVersion(Integer defaultVersion) {
      this.defaultVersion = defaultVersion;
      return this;
    }

    public Builder withEnvIdVersionMap(Map<String, EntityVersion> envIdVersionMap) {
      this.envIdVersionMap = envIdVersionMap;
      return this;
    }

    public Builder withTargetToAllEnv(boolean targetToAllEnv) {
      this.targetToAllEnv = targetToAllEnv;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withServices(List<Service> services) {
      this.services = services;
      return this;
    }

    public Builder withGraph(Graph graph) {
      this.graph = graph;
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

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
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
