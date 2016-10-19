package software.wings.sm;

import static org.apache.commons.lang3.RandomUtils.nextInt;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ServiceElement;
import software.wings.api.WorkflowElement;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ErrorStrategy;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.artifact.Artifact;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.SettingsService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class WorkflowStandardParams.
 *
 * @author Rishi.
 */
public class WorkflowStandardParams implements ContextElement {
  private static final String STANDARD_PARAMS = "STANDARD_PARAMS";

  @Inject private AppService appService;

  @Inject private ArtifactService artifactService;

  @Inject private EnvironmentService environmentService;

  @Inject private SettingsService settingsService;

  private String appId;
  private String envId;
  private List<String> artifactIds;
  private WorkflowElement workflowElement;

  // TODO: centralized in-memory executionCredential and special encrypted mapping
  private ExecutionCredential executionCredential;

  @JsonIgnore @Transient private transient Application app;
  @JsonIgnore @Transient private transient Environment env;
  @JsonIgnore @Transient private transient List<Artifact> artifacts;

  private List<ServiceElement> services;

  private ErrorStrategy errorStrategy;

  private Long startTs;
  private Long endTs;

  private String timestampId = System.currentTimeMillis() + "-" + nextInt(0, 1000);

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, Object> paramMap() {
    Map<String, Object> map = new HashMap<>();
    if (workflowElement != null) {
      map.put(WORKFLOW, workflowElement);
    }
    map.put(APP, getApp());
    map.put(ENV, getEnv());
    map.put(TIMESTAMP_ID, timestampId);

    return map;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ContextElementType getElementType() {
    return ContextElementType.STANDARD;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return STANDARD_PARAMS;
  }

  /**
   * Gets app id.
   *
   * @return the app id
   */
  public String getAppId() {
    return appId;
  }

  /**
   * Sets app id.
   *
   * @param appId the app id
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
   * Gets env id.
   *
   * @return the env id
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets env id.
   *
   * @param envId the env id
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Gets artifact ids.
   *
   * @return the artifact ids
   */
  public List<String> getArtifactIds() {
    return artifactIds;
  }

  /**
   * Sets artifact ids.
   *
   * @param artifactIds the artifact ids
   */
  public void setArtifactIds(List<String> artifactIds) {
    this.artifactIds = artifactIds;
  }

  /**
   * Gets start ts.
   *
   * @return the start ts
   */
  public Long getStartTs() {
    return startTs;
  }

  /**
   * Sets start ts.
   *
   * @param startTs the start ts
   */
  public void setStartTs(Long startTs) {
    this.startTs = startTs;
  }

  /**
   * Gets end ts.
   *
   * @return the end ts
   */
  public Long getEndTs() {
    return endTs;
  }

  /**
   * Sets end ts.
   *
   * @param endTs the end ts
   */
  public void setEndTs(Long endTs) {
    this.endTs = endTs;
  }

  /**
   * Gets timestamp id.
   *
   * @return the timestamp id
   */
  public String getTimestampId() {
    return timestampId;
  }

  /**
   * Sets timestamp id.
   *
   * @param timestampId the timestamp id
   */
  public void setTimestampId(String timestampId) {
    this.timestampId = timestampId;
  }

  /**
   * Gets execution credential.
   *
   * @return the execution credential
   */
  public ExecutionCredential getExecutionCredential() {
    return executionCredential;
  }

  /**
   * Sets execution credential.
   *
   * @param executionCredential the execution credential
   */
  public void setExecutionCredential(ExecutionCredential executionCredential) {
    this.executionCredential = executionCredential;
  }

  /**
   * Gets services.
   *
   * @return the services
   */
  public List<ServiceElement> getServices() {
    return services;
  }

  /**
   * Sets services.
   *
   * @param services the services
   */
  public void setServices(List<ServiceElement> services) {
    this.services = services;
  }

  /**
   * Gets error strategy.
   *
   * @return the error strategy
   */
  public ErrorStrategy getErrorStrategy() {
    return errorStrategy;
  }

  /**
   * Sets error strategy.
   *
   * @param errorStrategy the error strategy
   */
  public void setErrorStrategy(ErrorStrategy errorStrategy) {
    this.errorStrategy = errorStrategy;
  }

  /**
   * Gets workflow element.
   *
   * @return the workflow element
   */
  public WorkflowElement getWorkflowElement() {
    return workflowElement;
  }

  /**
   * Sets workflow element.
   *
   * @param workflowElement the workflow element
   */
  public void setWorkflowElement(WorkflowElement workflowElement) {
    this.workflowElement = workflowElement;
  }

  /**
   * Gets app.
   *
   * @return the app
   */
  public Application getApp() {
    if (app == null && appId != null) {
      app = appService.get(appId);
    }
    return app;
  }

  /**
   * Gets env.
   *
   * @return the env
   */
  public Environment getEnv() {
    if (env == null && envId != null) {
      env = environmentService.get(appId, envId, false);
    }
    return env;
  }

  /**
   * Gets artifacts.
   *
   * @return the artifacts
   */
  public List<Artifact> getArtifacts() {
    if (artifacts == null && artifactIds != null && artifactIds.size() > 0) {
      List<Artifact> list = new ArrayList<>();
      for (String artifactId : artifactIds) {
        list.add(artifactService.get(appId, artifactId));
      }
      artifacts = list;
    }
    return artifacts;
  }

  /**
   * Gets artifact for service.
   *
   * @param serviceId the service id
   * @return the artifact for service
   */
  public Artifact getArtifactForService(String serviceId) {
    return getArtifacts()
        .stream()
        .filter(artifact -> artifact.getServiceIds().contains(serviceId))
        .findFirst()
        .orElse(null);
  }

  @Override
  public String getUuid() {
    return null;
  }

  /**
   * Sets uuid.
   *
   * @param uuid the uuid
   */
  public void setUuid(String uuid) {}

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String appId;
    private String envId;
    private List<String> artifactIds;
    // TODO: centralized in-memory executionCredential and special encrypted mapping
    private ExecutionCredential executionCredential;
    private List<ServiceElement> services;
    private Long startTs;
    private Long endTs;
    private String timestampId = System.currentTimeMillis() + "-" + nextInt(0, 1000);

    private Builder() {}

    /**
     * A workflow standard params builder.
     *
     * @return the builder
     */
    public static Builder aWorkflowStandardParams() {
      return new Builder();
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
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With artifact ids builder.
     *
     * @param artifactIds the artifact ids
     * @return the builder
     */
    public Builder withArtifactIds(List<String> artifactIds) {
      this.artifactIds = artifactIds;
      return this;
    }

    /**
     * With execution credential builder.
     *
     * @param executionCredential the execution credential
     * @return the builder
     */
    public Builder withExecutionCredential(ExecutionCredential executionCredential) {
      this.executionCredential = executionCredential;
      return this;
    }

    /**
     * With services builder.
     *
     * @param services the services
     * @return the builder
     */
    public Builder withServices(List<ServiceElement> services) {
      this.services = services;
      return this;
    }

    /**
     * With start ts builder.
     *
     * @param startTs the start ts
     * @return the builder
     */
    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    /**
     * With end ts builder.
     *
     * @param endTs the end ts
     * @return the builder
     */
    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    /**
     * With timestamp id builder.
     *
     * @param timestampId the timestamp id
     * @return the builder
     */
    public Builder withTimestampId(String timestampId) {
      this.timestampId = timestampId;
      return this;
    }

    /**
     * Build workflow standard params.
     *
     * @return the workflow standard params
     */
    public WorkflowStandardParams build() {
      WorkflowStandardParams workflowStandardParams = new WorkflowStandardParams();
      workflowStandardParams.setAppId(appId);
      workflowStandardParams.setEnvId(envId);
      workflowStandardParams.setArtifactIds(artifactIds);
      workflowStandardParams.setExecutionCredential(executionCredential);
      workflowStandardParams.setServices(services);
      workflowStandardParams.setStartTs(startTs);
      workflowStandardParams.setEndTs(endTs);
      workflowStandardParams.setTimestampId(timestampId);
      return workflowStandardParams;
    }
  }
}
