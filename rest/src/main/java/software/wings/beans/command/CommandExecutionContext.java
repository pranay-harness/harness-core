package software.wings.beans.command;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import software.wings.beans.AppContainer;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.infrastructure.Host;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by peeyushaggarwal on 6/9/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CommandExecutionContext {
  private String accountId;
  private String envId;
  private Host host;
  private String appId;
  private String activityId;
  private String serviceName;
  private String runtimePath;
  private String stagingPath;
  private String backupPath;
  private String serviceTemplateId;
  private ExecutionCredential executionCredential;
  private AppContainer appContainer;
  private List<ArtifactFile> artifactFiles;
  private Map<String, String> serviceVariables = Maps.newHashMap();
  private Map<String, String> safeDisplayServiceVariables = Maps.newHashMap();
  private Map<String, String> envVariables = Maps.newHashMap();
  private SettingAttribute hostConnectionAttributes;
  private List<EncryptedDataDetail> hostConnectionCredentials;
  private SettingAttribute bastionConnectionAttributes;
  private List<EncryptedDataDetail> bastionConnectionCredentials;
  private ArtifactStreamAttributes artifactStreamAttributes;
  private SettingAttribute cloudProviderSetting;
  private List<EncryptedDataDetail> cloudProviderCredentials;
  private CodeDeployParams codeDeployParams;
  private ContainerSetupParams containerSetupParams;
  private ContainerResizeParams containerResizeParams;
  private Map<String, String> metadata = Maps.newHashMap();
  private CommandExecutionData commandExecutionData;
  private Integer timeout;
  private String deploymentType;

  public CommandExecutionContext() {}

  /**
   * Instantiates a new Command execution context.
   *
   * @param other the other
   */
  public CommandExecutionContext(CommandExecutionContext other) {
    this.accountId = other.accountId;
    this.envId = other.envId;
    this.host = other.host;
    this.appId = other.appId;
    this.activityId = other.activityId;
    this.runtimePath = other.runtimePath;
    this.stagingPath = other.stagingPath;
    this.backupPath = other.backupPath;
    this.serviceTemplateId = other.serviceTemplateId;
    this.executionCredential = other.executionCredential;
    this.appContainer = other.appContainer;
    this.artifactFiles = other.artifactFiles;
    this.serviceVariables = other.serviceVariables;
    this.safeDisplayServiceVariables = other.safeDisplayServiceVariables;
    this.envVariables = other.envVariables;
    this.hostConnectionAttributes = other.hostConnectionAttributes;
    this.hostConnectionCredentials = other.hostConnectionCredentials;
    this.bastionConnectionAttributes = other.bastionConnectionAttributes;
    this.bastionConnectionCredentials = other.bastionConnectionCredentials;
    this.artifactStreamAttributes = other.artifactStreamAttributes;
    this.cloudProviderSetting = other.cloudProviderSetting;
    this.cloudProviderCredentials = other.cloudProviderCredentials;
    this.codeDeployParams = other.codeDeployParams;
    this.containerSetupParams = other.containerSetupParams;
    this.containerResizeParams = other.containerResizeParams;
    this.metadata = other.metadata;
    this.commandExecutionData = other.commandExecutionData;
    this.timeout = other.timeout;
    this.deploymentType = other.deploymentType;
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
        text = text.replaceAll("\\$\\{" + key + "}", value);
        text = text.replaceAll("\\$" + key, value);
      }
    }
    return text;
  }

  public static final class Builder {
    private CommandExecutionContext commandExecutionContext;

    private Builder() {
      commandExecutionContext = new CommandExecutionContext();
    }

    public static Builder aCommandExecutionContext() {
      return new Builder();
    }

    public Builder withAccountId(String accountId) {
      commandExecutionContext.setAccountId(accountId);
      return this;
    }

    public Builder withEnvId(String envId) {
      commandExecutionContext.setEnvId(envId);
      return this;
    }

    public Builder withHost(Host host) {
      commandExecutionContext.setHost(host);
      return this;
    }

    public Builder withAppId(String appId) {
      commandExecutionContext.setAppId(appId);
      return this;
    }

    public Builder withActivityId(String activityId) {
      commandExecutionContext.setActivityId(activityId);
      return this;
    }

    public Builder withServiceName(String serviceName) {
      commandExecutionContext.setServiceName(serviceName);
      return this;
    }

    public Builder withRuntimePath(String runtimePath) {
      commandExecutionContext.setRuntimePath(runtimePath);
      return this;
    }

    public Builder withStagingPath(String stagingPath) {
      commandExecutionContext.setStagingPath(stagingPath);
      return this;
    }

    public Builder withBackupPath(String backupPath) {
      commandExecutionContext.setBackupPath(backupPath);
      return this;
    }

    public Builder withServiceTemplateId(String serviceTemplateId) {
      commandExecutionContext.setServiceTemplateId(serviceTemplateId);
      return this;
    }

    public Builder withExecutionCredential(ExecutionCredential executionCredential) {
      commandExecutionContext.setExecutionCredential(executionCredential);
      return this;
    }

    public Builder withAppContainer(AppContainer appContainer) {
      commandExecutionContext.setAppContainer(appContainer);
      return this;
    }

    public Builder withArtifactFiles(List<ArtifactFile> artifactFiles) {
      commandExecutionContext.setArtifactFiles(artifactFiles);
      return this;
    }

    public Builder withServiceVariables(Map<String, String> serviceVariables) {
      commandExecutionContext.setServiceVariables(serviceVariables);
      return this;
    }

    public Builder withSafeDisplayServiceVariables(Map<String, String> safeDisplayServiceVariables) {
      commandExecutionContext.setSafeDisplayServiceVariables(safeDisplayServiceVariables);
      return this;
    }

    public Builder withEnvVariables(Map<String, String> envVariables) {
      commandExecutionContext.setEnvVariables(envVariables);
      return this;
    }

    public Builder withHostConnectionAttributes(SettingAttribute hostConnectionAttributes) {
      commandExecutionContext.setHostConnectionAttributes(hostConnectionAttributes);
      return this;
    }

    public Builder withHostConnectionCredentials(List<EncryptedDataDetail> hostConnectionCredentials) {
      commandExecutionContext.setHostConnectionCredentials(hostConnectionCredentials);
      return this;
    }

    public Builder withBastionConnectionAttributes(SettingAttribute bastionConnectionAttributes) {
      commandExecutionContext.setBastionConnectionAttributes(bastionConnectionAttributes);
      return this;
    }

    public Builder withBastionConnectionCredentials(List<EncryptedDataDetail> bastionConnectionCredentials) {
      commandExecutionContext.setBastionConnectionCredentials(bastionConnectionCredentials);
      return this;
    }

    public Builder withArtifactStreamAttributes(ArtifactStreamAttributes artifactStreamAttributes) {
      commandExecutionContext.setArtifactStreamAttributes(artifactStreamAttributes);
      return this;
    }

    public Builder withCloudProviderSetting(SettingAttribute cloudProviderSetting) {
      commandExecutionContext.setCloudProviderSetting(cloudProviderSetting);
      return this;
    }

    public Builder withCloudProviderCredentials(List<EncryptedDataDetail> cloudProviderCredentials) {
      commandExecutionContext.setCloudProviderCredentials(cloudProviderCredentials);
      return this;
    }

    public Builder withCodeDeployParams(CodeDeployParams codeDeployParams) {
      commandExecutionContext.setCodeDeployParams(codeDeployParams);
      return this;
    }

    public Builder withContainerSetupParams(ContainerSetupParams containerSetupParams) {
      commandExecutionContext.setContainerSetupParams(containerSetupParams);
      return this;
    }

    public Builder withContainerResizeParams(ContainerResizeParams containerResizeParams) {
      commandExecutionContext.setContainerResizeParams(containerResizeParams);
      return this;
    }

    public Builder withMetadata(Map<String, String> metadata) {
      commandExecutionContext.setMetadata(metadata);
      return this;
    }

    public Builder withCommandExecutionData(CommandExecutionData commandExecutionData) {
      commandExecutionContext.setCommandExecutionData(commandExecutionData);
      return this;
    }

    public Builder withTimeout(Integer timeout) {
      commandExecutionContext.setTimeout(timeout);
      return this;
    }

    public Builder withDeploymentType(String deploymentType) {
      commandExecutionContext.setDeploymentType(deploymentType);
      return this;
    }

    public Builder but() {
      return aCommandExecutionContext()
          .withAccountId(commandExecutionContext.getAccountId())
          .withEnvId(commandExecutionContext.getEnvId())
          .withHost(commandExecutionContext.getHost())
          .withAppId(commandExecutionContext.getAppId())
          .withActivityId(commandExecutionContext.getActivityId())
          .withServiceName(commandExecutionContext.getServiceName())
          .withRuntimePath(commandExecutionContext.getRuntimePath())
          .withStagingPath(commandExecutionContext.getStagingPath())
          .withBackupPath(commandExecutionContext.getBackupPath())
          .withServiceTemplateId(commandExecutionContext.getServiceTemplateId())
          .withExecutionCredential(commandExecutionContext.getExecutionCredential())
          .withAppContainer(commandExecutionContext.getAppContainer())
          .withArtifactFiles(commandExecutionContext.getArtifactFiles())
          .withServiceVariables(commandExecutionContext.getServiceVariables())
          .withSafeDisplayServiceVariables(commandExecutionContext.getSafeDisplayServiceVariables())
          .withEnvVariables(commandExecutionContext.getEnvVariables())
          .withHostConnectionAttributes(commandExecutionContext.getHostConnectionAttributes())
          .withHostConnectionCredentials(commandExecutionContext.getHostConnectionCredentials())
          .withBastionConnectionAttributes(commandExecutionContext.getBastionConnectionAttributes())
          .withBastionConnectionCredentials(commandExecutionContext.getBastionConnectionCredentials())
          .withArtifactStreamAttributes(commandExecutionContext.getArtifactStreamAttributes())
          .withCloudProviderSetting(commandExecutionContext.getCloudProviderSetting())
          .withCloudProviderCredentials(commandExecutionContext.getCloudProviderCredentials())
          .withCodeDeployParams(commandExecutionContext.getCodeDeployParams())
          .withContainerSetupParams(commandExecutionContext.getContainerSetupParams())
          .withContainerResizeParams(commandExecutionContext.getContainerResizeParams())
          .withMetadata(commandExecutionContext.getMetadata())
          .withCommandExecutionData(commandExecutionContext.getCommandExecutionData())
          .withTimeout(commandExecutionContext.getTimeout())
          .withDeploymentType(commandExecutionContext.getDeploymentType());
    }

    public CommandExecutionContext build() {
      return commandExecutionContext;
    }
  }
}
