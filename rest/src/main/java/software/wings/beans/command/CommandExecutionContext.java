package software.wings.beans.command;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.AbstractCommandUnit.ExecutionResult;
import software.wings.beans.infrastructure.Host;
import software.wings.service.intfc.FileService.FileBucket;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Created by peeyushaggarwal on 6/9/16.
 */
public class CommandExecutionContext {
  private String accountId;
  private List<ArtifactFile> artifactFiles;
  private String envId;
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
  private SettingAttribute hostConnectionAttributes;
  private SettingAttribute bastionConnectionAttributes;
  private ArtifactStreamAttributes artifactStreamAttributes;

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
    this.accountId = other.accountId;
    this.artifactFiles = other.artifactFiles;
    this.envId = other.envId;
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
    this.hostConnectionAttributes = other.hostConnectionAttributes;
    this.bastionConnectionAttributes = other.bastionConnectionAttributes;
    this.artifactStreamAttributes = other.artifactStreamAttributes;
  }

  /**
   * Getter for property 'artifactFiles'.
   *
   * @return Value for property 'artifactFiles'.
   */
  public List<ArtifactFile> getArtifactFiles() {
    return artifactFiles;
  }

  /**
   * Setter for property 'artifactFiles'.
   *
   * @param artifactFiles Value to set for property 'artifactFiles'.
   */
  public void setArtifactFiles(List<ArtifactFile> artifactFiles) {
    this.artifactFiles = artifactFiles;
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
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets service instance.
   *
   * @param envId the service instance
   */
  public void setEnvId(String envId) {
    this.envId = envId;
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

  /**
   * Getter for property 'hostConnectionAttributes'.
   *
   * @return Value for property 'hostConnectionAttributes'.
   */
  public SettingAttribute getHostConnectionAttributes() {
    return hostConnectionAttributes;
  }

  /**
   * Setter for property 'hostConnectionAttributes'.
   *
   * @param hostConnectionAttributes Value to set for property 'hostConnectionAttributes'.
   */
  public void setHostConnectionAttributes(SettingAttribute hostConnectionAttributes) {
    this.hostConnectionAttributes = hostConnectionAttributes;
  }

  /**
   * Getter for property 'bastionConnectionAttributes'.
   *
   * @return Value for property 'bastionConnectionAttributes'.
   */
  public SettingAttribute getBastionConnectionAttributes() {
    return bastionConnectionAttributes;
  }

  /**
   * Setter for property 'bastionConnectionAttributes'.
   *
   * @param bastionConnectionAttributes Value to set for property 'bastionConnectionAttributes'.
   */
  public void setBastionConnectionAttributes(SettingAttribute bastionConnectionAttributes) {
    this.bastionConnectionAttributes = bastionConnectionAttributes;
  }

  /**
   * Getter for property 'accountId'.
   *
   * @return Value for property 'accountId'.
   */
  public String getAccountId() {
    return accountId;
  }

  /**
   * Setter for property 'accountId'.
   *
   * @param accountId Value to set for property 'accountId'.
   */
  public void setAccountId(String accountId) {
    this.accountId = accountId;
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

  /**
   * Gets artifact stream attributes.
   *
   * @return the artifact stream attributes
   */
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return artifactStreamAttributes;
  }

  /**
   * Sets artifact stream attributes.
   *
   * @param artifactStreamAttributes the artifact stream attributes
   */
  public void setArtifactStreamAttributes(ArtifactStreamAttributes artifactStreamAttributes) {
    this.artifactStreamAttributes = artifactStreamAttributes;
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountId, artifactFiles, envId, host, serviceTemplate, appId, activityId, runtimePath,
        stagingPath, backupPath, executionCredential, serviceVariables, envVariables, hostConnectionAttributes,
        bastionConnectionAttributes, artifactStreamAttributes);
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
    return Objects.equals(this.accountId, other.accountId) && Objects.equals(this.artifactFiles, other.artifactFiles)
        && Objects.equals(this.envId, other.envId) && Objects.equals(this.host, other.host)
        && Objects.equals(this.serviceTemplate, other.serviceTemplate) && Objects.equals(this.appId, other.appId)
        && Objects.equals(this.activityId, other.activityId) && Objects.equals(this.runtimePath, other.runtimePath)
        && Objects.equals(this.stagingPath, other.stagingPath) && Objects.equals(this.backupPath, other.backupPath)
        && Objects.equals(this.executionCredential, other.executionCredential)
        && Objects.equals(this.serviceVariables, other.serviceVariables)
        && Objects.equals(this.envVariables, other.envVariables)
        && Objects.equals(this.hostConnectionAttributes, other.hostConnectionAttributes)
        && Objects.equals(this.bastionConnectionAttributes, other.bastionConnectionAttributes)
        && Objects.equals(this.artifactStreamAttributes, other.artifactStreamAttributes);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("accountId", accountId)
        .add("artifactFiles", artifactFiles)
        .add("envId", envId)
        .add("host", host)
        .add("serviceTemplate", serviceTemplate)
        .add("appId", appId)
        .add("activityId", activityId)
        .add("runtimePath", runtimePath)
        .add("stagingPath", stagingPath)
        .add("backupPath", backupPath)
        .add("executionCredential", executionCredential)
        .add("serviceVariables", serviceVariables)
        .add("envVariables", envVariables)
        .add("hostConnectionAttributes", hostConnectionAttributes)
        .add("bastionConnectionAttributes", bastionConnectionAttributes)
        .add("artifactStreamAttributes", artifactStreamAttributes)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String accountId;
    private List<ArtifactFile> artifactFiles;
    private String envId;
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
    private SettingAttribute hostConnectionAttributes;
    private SettingAttribute bastionConnectionAttributes;
    private ArtifactStreamAttributes artifactStreamAttributes;

    private Builder() {}

    /**
     * A command execution context builder.
     *
     * @return the builder
     */
    public static Builder aCommandExecutionContext() {
      return new Builder();
    }

    /**
     * With account id builder.
     *
     * @param accountId the account id
     * @return the builder
     */
    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    /**
     * With artifact files builder.
     *
     * @param artifactFiles the artifact files
     * @return the builder
     */
    public Builder withArtifactFiles(List<ArtifactFile> artifactFiles) {
      this.artifactFiles = artifactFiles;
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
     * With host builder.
     *
     * @param host the host
     * @return the builder
     */
    public Builder withHost(Host host) {
      this.host = host;
      return this;
    }

    /**
     * With service template builder.
     *
     * @param serviceTemplate the service template
     * @return the builder
     */
    public Builder withServiceTemplate(ServiceTemplate serviceTemplate) {
      this.serviceTemplate = serviceTemplate;
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
     * With activity id builder.
     *
     * @param activityId the activity id
     * @return the builder
     */
    public Builder withActivityId(String activityId) {
      this.activityId = activityId;
      return this;
    }

    /**
     * With runtime path builder.
     *
     * @param runtimePath the runtime path
     * @return the builder
     */
    public Builder withRuntimePath(String runtimePath) {
      this.runtimePath = runtimePath;
      return this;
    }

    /**
     * With staging path builder.
     *
     * @param stagingPath the staging path
     * @return the builder
     */
    public Builder withStagingPath(String stagingPath) {
      this.stagingPath = stagingPath;
      return this;
    }

    /**
     * With backup path builder.
     *
     * @param backupPath the backup path
     * @return the builder
     */
    public Builder withBackupPath(String backupPath) {
      this.backupPath = backupPath;
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
     * With service variables builder.
     *
     * @param serviceVariables the service variables
     * @return the builder
     */
    public Builder withServiceVariables(Map<String, String> serviceVariables) {
      this.serviceVariables = serviceVariables;
      return this;
    }

    /**
     * With env variables builder.
     *
     * @param envVariables the env variables
     * @return the builder
     */
    public Builder withEnvVariables(Map<String, String> envVariables) {
      this.envVariables = envVariables;
      return this;
    }

    /**
     * With host connection attributes builder.
     *
     * @param hostConnectionAttributes the host connection attributes
     * @return the builder
     */
    public Builder withHostConnectionAttributes(SettingAttribute hostConnectionAttributes) {
      this.hostConnectionAttributes = hostConnectionAttributes;
      return this;
    }

    /**
     * With bastion connection attributes builder.
     *
     * @param bastionConnectionAttributes the bastion connection attributes
     * @return the builder
     */
    public Builder withBastionConnectionAttributes(SettingAttribute bastionConnectionAttributes) {
      this.bastionConnectionAttributes = bastionConnectionAttributes;
      return this;
    }

    /**
     * With artifact stream attributes builder.
     *
     * @param artifactStreamAttributes the artifact stream attributes
     * @return the builder
     */
    public Builder withArtifactStreamAttributes(ArtifactStreamAttributes artifactStreamAttributes) {
      this.artifactStreamAttributes = artifactStreamAttributes;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aCommandExecutionContext()
          .withAccountId(accountId)
          .withArtifactFiles(artifactFiles)
          .withEnvId(envId)
          .withHost(host)
          .withServiceTemplate(serviceTemplate)
          .withAppId(appId)
          .withActivityId(activityId)
          .withRuntimePath(runtimePath)
          .withStagingPath(stagingPath)
          .withBackupPath(backupPath)
          .withExecutionCredential(executionCredential)
          .withServiceVariables(serviceVariables)
          .withEnvVariables(envVariables)
          .withHostConnectionAttributes(hostConnectionAttributes)
          .withBastionConnectionAttributes(bastionConnectionAttributes)
          .withArtifactStreamAttributes(artifactStreamAttributes);
    }

    /**
     * Build command execution context.
     *
     * @return the command execution context
     */
    public CommandExecutionContext build() {
      CommandExecutionContext commandExecutionContext = new CommandExecutionContext();
      commandExecutionContext.setAccountId(accountId);
      commandExecutionContext.setArtifactFiles(artifactFiles);
      commandExecutionContext.setEnvId(envId);
      commandExecutionContext.setHost(host);
      commandExecutionContext.setServiceTemplate(serviceTemplate);
      commandExecutionContext.setAppId(appId);
      commandExecutionContext.setActivityId(activityId);
      commandExecutionContext.setRuntimePath(runtimePath);
      commandExecutionContext.setStagingPath(stagingPath);
      commandExecutionContext.setBackupPath(backupPath);
      commandExecutionContext.setExecutionCredential(executionCredential);
      commandExecutionContext.setServiceVariables(serviceVariables);
      commandExecutionContext.setHostConnectionAttributes(hostConnectionAttributes);
      commandExecutionContext.setBastionConnectionAttributes(bastionConnectionAttributes);
      commandExecutionContext.setArtifactStreamAttributes(artifactStreamAttributes);
      commandExecutionContext.envVariables = this.envVariables;
      return commandExecutionContext;
    }
  }
}
