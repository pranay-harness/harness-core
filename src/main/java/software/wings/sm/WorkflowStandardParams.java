package software.wings.sm;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Application;
import software.wings.beans.Artifact;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.Service;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.SettingsService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

// TODO: Auto-generated Javadoc

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

  // TODO: centralized in-memory executionCredential and special encrypted mapping
  private ExecutionCredential executionCredential;

  @JsonIgnore @Transient private transient Application app;
  @JsonIgnore @Transient private transient Environment env;
  @JsonIgnore @Transient private transient List<Artifact> artifacts;

  private Long startTs;
  private Long endTs;

  private String timestampId = System.currentTimeMillis() + "-" + new Random().nextInt(1000);

  /** {@inheritDoc} */
  @Override
  public Map<String, Object> paramMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(APP, getApp());
    map.put(ENV, getEnv());
    map.put(TIMESTAMP_ID, timestampId);

    return map;
  }

  /** {@inheritDoc} */
  @Override
  public ContextElementType getElementType() {
    return ContextElementType.STANDARD;
  }

  /** {@inheritDoc} */
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

  public String getTimestampId() {
    return timestampId;
  }

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
   * Gets app.
   *
   * @return the app
   */
  public Application getApp() {
    if (app == null && appId != null) {
      app = appService.findByUuid(appId);
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
      env = environmentService.get(appId, envId);
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

  public Artifact getArtifactForService(Service service) {
    return artifacts.stream().filter(artifact -> artifact.getSevices().contains(service)).findFirst().orElse(null);
  }

  public static final class Builder {
    private String appId;
    private String envId;
    private List<String> artifactIds;
    // TODO: centralized in-memory executionCredential and special encrypted mapping
    private ExecutionCredential executionCredential;
    private Long startTs;
    private Long endTs;
    private String timestampId = System.currentTimeMillis() + "-" + new Random().nextInt(1000);

    private Builder() {}

    public static Builder aWorkflowStandardParams() {
      return new Builder();
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public Builder withArtifactIds(List<String> artifactIds) {
      this.artifactIds = artifactIds;
      return this;
    }

    public Builder withExecutionCredential(ExecutionCredential executionCredential) {
      this.executionCredential = executionCredential;
      return this;
    }

    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public Builder withTimestampId(String timestampId) {
      this.timestampId = timestampId;
      return this;
    }

    public Builder but() {
      return aWorkflowStandardParams()
          .withAppId(appId)
          .withEnvId(envId)
          .withArtifactIds(artifactIds)
          .withExecutionCredential(executionCredential)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withTimestampId(timestampId);
    }

    public WorkflowStandardParams build() {
      WorkflowStandardParams workflowStandardParams = new WorkflowStandardParams();
      workflowStandardParams.setAppId(appId);
      workflowStandardParams.setEnvId(envId);
      workflowStandardParams.setArtifactIds(artifactIds);
      workflowStandardParams.setExecutionCredential(executionCredential);
      workflowStandardParams.setStartTs(startTs);
      workflowStandardParams.setEndTs(endTs);
      workflowStandardParams.setTimestampId(timestampId);
      return workflowStandardParams;
    }
  }
}
