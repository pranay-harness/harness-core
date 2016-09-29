package software.wings.beans.command;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.Artifact;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.Host;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.command.CommandUnit.ExecutionResult;
import software.wings.service.intfc.FileService.FileBucket;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Created by peeyushaggarwal on 6/9/16.
 */
public class CommandExecutionContext {
  private Artifact artifact;
  private ServiceInstance serviceInstance;
  private Host host;
  private ServiceTemplate serviceTemplate;
  private String appId;
  private String activityId;
  private String runtimePath;
  private String stagingPath;
  private String backupPath;
  private ExecutionCredential executionCredential;
  private Map<String, String> serviceVariables = Maps.newHashMap();
  private Map<String, String> envVariables = Maps.newHashMap();

  /**
   * Instantiates a new Command execution context.
   */
  public CommandExecutionContext() {}

  /**
   * Instantiates a new Command execution context.
   *
   * @param other the other
   */
  public CommandExecutionContext(CommandExecutionContext other) {
    this.artifact = other.artifact;
    this.serviceInstance = other.serviceInstance;
    this.appId = other.appId;
    this.activityId = other.activityId;
    this.runtimePath = other.runtimePath;
    this.stagingPath = other.stagingPath;
    this.backupPath = other.backupPath;
    this.executionCredential = other.executionCredential;
    this.envVariables = other.envVariables;
    this.serviceVariables = other.serviceVariables;
    this.host = other.host;
    this.serviceTemplate = other.serviceTemplate;
  }

  /**
   * Gets artifact.
   *
   * @return the artifact
   */
  public Artifact getArtifact() {
    return artifact;
  }

  /**
   * Sets artifact.
   *
   * @param artifact the artifact
   */
  public void setArtifact(Artifact artifact) {
    this.artifact = artifact;
  }

  /**
   * Gets activity id.
   *
   * @return the activity id
   */
  public String getActivityId() {
    return activityId;
  }

  /**
   * Sets activity id.
   *
   * @param activityId the activity id
   */
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  /**
   * Gets runtime path.
   *
   * @return the runtime path
   */
  public String getRuntimePath() {
    return runtimePath;
  }

  /**
   * Sets runtime path.
   *
   * @param runtimePath the runtime path
   */
  public void setRuntimePath(String runtimePath) {
    this.runtimePath = runtimePath;
  }

  /**
   * Gets staging path.
   *
   * @return the staging path
   */
  public String getStagingPath() {
    return stagingPath;
  }

  /**
   * Sets staging path.
   *
   * @param stagingPath the staging path
   */
  public void setStagingPath(String stagingPath) {
    this.stagingPath = stagingPath;
  }

  /**
   * Gets backup path.
   *
   * @return the backup path
   */
  public String getBackupPath() {
    return backupPath;
  }

  /**
   * Sets backup path.
   *
   * @param backupPath the backup path
   */
  public void setBackupPath(String backupPath) {
    this.backupPath = backupPath;
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
   * Gets service instance.
   *
   * @return the service instance
   */
  public ServiceInstance getServiceInstance() {
    return serviceInstance;
  }

  /**
   * Sets service instance.
   *
   * @param serviceInstance the service instance
   */
  public void setServiceInstance(ServiceInstance serviceInstance) {
    this.serviceInstance = serviceInstance;
  }

  /**
   * Getter for property 'serviceVariables'.
   *
   * @return Value for property 'serviceVariables'.
   */
  public Map<String, String> getServiceVariables() {
    return serviceVariables;
  }

  /**
   * Setter for property 'serviceVariables'.
   *
   * @param serviceVariables Value to set for property 'serviceVariables'.
   */
  public void setServiceVariables(Map<String, String> serviceVariables) {
    this.serviceVariables = serviceVariables;
  }

  /**
   * Getter for property 'host'.
   *
   * @return Value for property 'host'.
   */
  public Host getHost() {
    return host;
  }

  /**
   * Setter for property 'host'.
   *
   * @param host Value to set for property 'host'.
   */
  public void setHost(Host host) {
    this.host = host;
  }

  /**
   * Getter for property 'serviceTemplate'.
   *
   * @return Value for property 'serviceTemplate'.
   */
  public ServiceTemplate getServiceTemplate() {
    return serviceTemplate;
  }

  /**
   * Setter for property 'serviceTemplate'.
   *
   * @param serviceTemplate Value to set for property 'serviceTemplate'.
   */
  public void setServiceTemplate(ServiceTemplate serviceTemplate) {
    this.serviceTemplate = serviceTemplate;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        artifact, serviceInstance, appId, activityId, runtimePath, stagingPath, backupPath, executionCredential);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final CommandExecutionContext other = (CommandExecutionContext) obj;
    return Objects.equals(this.artifact, other.artifact) && Objects.equals(this.serviceInstance, other.serviceInstance)
        && Objects.equals(this.appId, other.appId) && Objects.equals(this.activityId, other.activityId)
        && Objects.equals(this.runtimePath, other.runtimePath) && Objects.equals(this.stagingPath, other.stagingPath)
        && Objects.equals(this.backupPath, other.backupPath)
        && Objects.equals(this.executionCredential, other.executionCredential);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("artifact", artifact)
        .add("serviceInstance", serviceInstance)
        .add("appId", appId)
        .add("activityId", activityId)
        .add("runtimePath", runtimePath)
        .add("stagingPath", stagingPath)
        .add("backupPath", backupPath)
        .add("executionCredential", executionCredential)
        .toString();
  }

  /**
   * Execute command string execution result.
   *
   * @param commandString the command string
   * @return the execution result
   */
  public ExecutionResult executeCommandString(String commandString) {
    throw new UnsupportedOperationException();
  }

  /**
   * Execute command string execution result.
   *
   * @param commandString the command string
   * @param output        the output
   * @return the execution result
   */
  public ExecutionResult executeCommandString(String commandString, StringBuffer output) {
    throw new UnsupportedOperationException();
  }

  /**
   * Copy files execution result.
   *
   * @param destinationDirectoryPath the destination directory path
   * @param files                    the files
   * @return the execution result
   */
  public ExecutionResult copyFiles(String destinationDirectoryPath, List<String> files) {
    throw new UnsupportedOperationException();
  }

  /**
   * Copy grid fs files execution result.
   *
   * @param destinationDirectoryPath the destination directory path
   * @param fileBucket               the file bucket
   * @param fileNamesIds             the file ids
   * @return the execution result
   */
  public ExecutionResult copyGridFsFiles(
      String destinationDirectoryPath, FileBucket fileBucket, List<Pair<String, String>> fileNamesIds) {
    throw new UnsupportedOperationException();
  }

  /**
   * Add env variables.
   *
   * @param envVariables the env variables
   */
  public void addEnvVariables(Map<String, String> envVariables) {
    for (Entry<String, String> envVariable : envVariables.entrySet()) {
      this.envVariables.put(envVariable.getKey(), evaluateVariable(envVariable.getValue()));
    }
  }

  /**
   * Evaluate variable string.
   *
   * @param text the text
   * @return the string
   */
  protected String evaluateVariable(String text) {
    if (isNotBlank(text)) {
      for (Entry<String, String> entry : envVariables.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        text = text.replaceAll("\\$\\{" + key + "\\}", value);
        text = text.replaceAll("\\$" + key, value);
      }
    }
    return text;
  }

  public static final class Builder {
    private Artifact artifact;
    private ServiceInstance serviceInstance;
    private Host host;
    private ServiceTemplate serviceTemplate;
    private String appId;
    private String activityId;
    private String runtimePath;
    private String stagingPath;
    private String backupPath;
    private ExecutionCredential executionCredential;
    private Map<String, String> serviceVariables = Maps.newHashMap();

    private Builder() {}

    public static Builder aCommandExecutionContext() {
      return new Builder();
    }

    public Builder withArtifact(Artifact artifact) {
      this.artifact = artifact;
      return this;
    }

    public Builder withServiceInstance(ServiceInstance serviceInstance) {
      this.serviceInstance = serviceInstance;
      return this;
    }

    public Builder withHost(Host host) {
      this.host = host;
      return this;
    }

    public Builder withServiceTemplate(ServiceTemplate serviceTemplate) {
      this.serviceTemplate = serviceTemplate;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withActivityId(String activityId) {
      this.activityId = activityId;
      return this;
    }

    public Builder withRuntimePath(String runtimePath) {
      this.runtimePath = runtimePath;
      return this;
    }

    public Builder withStagingPath(String stagingPath) {
      this.stagingPath = stagingPath;
      return this;
    }

    public Builder withBackupPath(String backupPath) {
      this.backupPath = backupPath;
      return this;
    }

    public Builder withExecutionCredential(ExecutionCredential executionCredential) {
      this.executionCredential = executionCredential;
      return this;
    }

    public Builder withServiceVariables(Map<String, String> serviceVariables) {
      this.serviceVariables = serviceVariables;
      return this;
    }

    public Builder but() {
      return aCommandExecutionContext()
          .withArtifact(artifact)
          .withServiceInstance(serviceInstance)
          .withHost(host)
          .withServiceTemplate(serviceTemplate)
          .withAppId(appId)
          .withActivityId(activityId)
          .withRuntimePath(runtimePath)
          .withStagingPath(stagingPath)
          .withBackupPath(backupPath)
          .withExecutionCredential(executionCredential)
          .withServiceVariables(serviceVariables);
    }

    public CommandExecutionContext build() {
      CommandExecutionContext commandExecutionContext = new CommandExecutionContext();
      commandExecutionContext.setArtifact(artifact);
      commandExecutionContext.setServiceInstance(serviceInstance);
      commandExecutionContext.setHost(host);
      commandExecutionContext.setServiceTemplate(serviceTemplate);
      commandExecutionContext.setAppId(appId);
      commandExecutionContext.setActivityId(activityId);
      commandExecutionContext.setRuntimePath(runtimePath);
      commandExecutionContext.setStagingPath(stagingPath);
      commandExecutionContext.setBackupPath(backupPath);
      commandExecutionContext.setExecutionCredential(executionCredential);
      commandExecutionContext.setServiceVariables(serviceVariables);
      return commandExecutionContext;
    }
  }
}
