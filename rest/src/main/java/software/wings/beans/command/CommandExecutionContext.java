package software.wings.beans.command;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import software.wings.api.ContainerServiceData;
import software.wings.beans.AppContainer;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.infrastructure.Host;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
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
  private String clusterName;
  private String namespace;
  private String serviceName;
  private String region;
  private int ecsServiceSteadyStateTimeout;
  private CodeDeployParams codeDeployParams;
  private Map<String, String> metadata = Maps.newHashMap();
  private List<ContainerServiceData> desiredCounts = new ArrayList<>();
  private CommandExecutionData commandExecutionData;

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
    this.clusterName = other.clusterName;
    this.namespace = other.namespace;
    this.serviceName = other.serviceName;
    this.region = other.region;
    this.codeDeployParams = other.codeDeployParams;
    this.metadata = other.metadata;
    this.desiredCounts = other.desiredCounts;
    this.commandExecutionData = other.commandExecutionData;
    this.ecsServiceSteadyStateTimeout = other.ecsServiceSteadyStateTimeout;
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
    private String accountId;
    private String envId;
    private Host host;
    private String appId;
    private String activityId;
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
    private String clusterName;
    private String namespace;
    private String serviceName;
    private String region;
    private int ecsServiceSteadyStateTimeout;
    private CodeDeployParams codeDeployParams;
    private Map<String, String> metadata = Maps.newHashMap();
    private List<ContainerServiceData> desiredCounts = new ArrayList<>();
    private CommandExecutionData commandExecutionData;

    private Builder() {}

    public static Builder aCommandExecutionContext() {
      return new Builder();
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public Builder withHost(Host host) {
      this.host = host;
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

    public Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    public Builder withExecutionCredential(ExecutionCredential executionCredential) {
      this.executionCredential = executionCredential;
      return this;
    }

    public Builder withAppContainer(AppContainer appContainer) {
      this.appContainer = appContainer;
      return this;
    }

    public Builder withArtifactFiles(List<ArtifactFile> artifactFiles) {
      this.artifactFiles = artifactFiles;
      return this;
    }

    public Builder withServiceVariables(Map<String, String> serviceVariables) {
      this.serviceVariables = serviceVariables;
      return this;
    }

    public Builder withSafeDisplayServiceVariables(Map<String, String> safeDisplayServiceVariables) {
      this.safeDisplayServiceVariables = safeDisplayServiceVariables;
      return this;
    }

    public Builder withEnvVariables(Map<String, String> envVariables) {
      this.envVariables = envVariables;
      return this;
    }

    public Builder withHostConnectionAttributes(SettingAttribute hostConnectionAttributes) {
      this.hostConnectionAttributes = hostConnectionAttributes;
      return this;
    }

    public Builder withHostConnectionCredentials(List<EncryptedDataDetail> encryptedDataDetails) {
      this.hostConnectionCredentials = encryptedDataDetails;
      return this;
    }

    public Builder withBastionConnectionAttributes(SettingAttribute bastionConnectionAttributes) {
      this.bastionConnectionAttributes = bastionConnectionAttributes;
      return this;
    }

    public Builder withBastionConnectionCredentials(List<EncryptedDataDetail> encryptedDataDetails) {
      this.bastionConnectionCredentials = encryptedDataDetails;
      return this;
    }

    public Builder withArtifactStreamAttributes(ArtifactStreamAttributes artifactStreamAttributes) {
      this.artifactStreamAttributes = artifactStreamAttributes;
      return this;
    }

    public Builder withCloudProviderSetting(SettingAttribute cloudProviderSetting) {
      this.cloudProviderSetting = cloudProviderSetting;
      return this;
    }

    public Builder withCloudProviderCredentials(List<EncryptedDataDetail> encryptedDataDetails) {
      this.cloudProviderCredentials = encryptedDataDetails;
      return this;
    }

    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder withNamespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    public Builder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder withEcsServiceSteadyStateTimeout(int ecsServiceSteadyStateTimeout) {
      this.ecsServiceSteadyStateTimeout = ecsServiceSteadyStateTimeout;
      return this;
    }

    public Builder withRegion(String region) {
      this.region = region;
      return this;
    }

    public Builder withCodeDeployParams(CodeDeployParams codeDeployParams) {
      this.codeDeployParams = codeDeployParams;
      return this;
    }

    public Builder withMetadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder withDesiredCounts(List<ContainerServiceData> desiredCounts) {
      this.desiredCounts = desiredCounts;
      return this;
    }

    public Builder withCommandExecutionData(CommandExecutionData commandExecutionData) {
      this.commandExecutionData = commandExecutionData;
      return this;
    }

    public Builder but() {
      return aCommandExecutionContext()
          .withAccountId(accountId)
          .withEnvId(envId)
          .withHost(host)
          .withAppId(appId)
          .withActivityId(activityId)
          .withRuntimePath(runtimePath)
          .withStagingPath(stagingPath)
          .withBackupPath(backupPath)
          .withServiceTemplateId(serviceTemplateId)
          .withExecutionCredential(executionCredential)
          .withAppContainer(appContainer)
          .withArtifactFiles(artifactFiles)
          .withServiceVariables(serviceVariables)
          .withEnvVariables(envVariables)
          .withHostConnectionAttributes(hostConnectionAttributes)
          .withBastionConnectionAttributes(bastionConnectionAttributes)
          .withHostConnectionCredentials(hostConnectionCredentials)
          .withBastionConnectionCredentials(bastionConnectionCredentials)
          .withArtifactStreamAttributes(artifactStreamAttributes)
          .withCloudProviderSetting(cloudProviderSetting)
          .withCloudProviderCredentials(cloudProviderCredentials)
          .withClusterName(clusterName)
          .withNamespace(namespace)
          .withServiceName(serviceName)
          .withRegion(region)
          .withCodeDeployParams(codeDeployParams)
          .withMetadata(metadata)
          .withDesiredCounts(desiredCounts)
          .withCommandExecutionData(commandExecutionData)
          .withEcsServiceSteadyStateTimeout(ecsServiceSteadyStateTimeout)
          .withSafeDisplayServiceVariables(safeDisplayServiceVariables);
    }

    public CommandExecutionContext build() {
      CommandExecutionContext commandExecutionContext = new CommandExecutionContext();
      commandExecutionContext.setAccountId(accountId);
      commandExecutionContext.setEnvId(envId);
      commandExecutionContext.setHost(host);
      commandExecutionContext.setAppId(appId);
      commandExecutionContext.setActivityId(activityId);
      commandExecutionContext.setRuntimePath(runtimePath);
      commandExecutionContext.setStagingPath(stagingPath);
      commandExecutionContext.setBackupPath(backupPath);
      commandExecutionContext.setServiceTemplateId(serviceTemplateId);
      commandExecutionContext.setExecutionCredential(executionCredential);
      commandExecutionContext.setAppContainer(appContainer);
      commandExecutionContext.setArtifactFiles(artifactFiles);
      commandExecutionContext.setServiceVariables(serviceVariables);
      commandExecutionContext.setSafeDisplayServiceVariables(safeDisplayServiceVariables);
      commandExecutionContext.setEnvVariables(envVariables);
      commandExecutionContext.setHostConnectionAttributes(hostConnectionAttributes);
      commandExecutionContext.setHostConnectionCredentials(hostConnectionCredentials);
      commandExecutionContext.setBastionConnectionAttributes(bastionConnectionAttributes);
      commandExecutionContext.setBastionConnectionCredentials(bastionConnectionCredentials);
      commandExecutionContext.setArtifactStreamAttributes(artifactStreamAttributes);
      commandExecutionContext.setCloudProviderSetting(cloudProviderSetting);
      commandExecutionContext.setCloudProviderCredentials(cloudProviderCredentials);
      commandExecutionContext.setClusterName(clusterName);
      commandExecutionContext.setNamespace(namespace);
      commandExecutionContext.setServiceName(serviceName);
      commandExecutionContext.setRegion(region);
      commandExecutionContext.setCodeDeployParams(codeDeployParams);
      commandExecutionContext.setMetadata(metadata);
      commandExecutionContext.setDesiredCounts(desiredCounts);
      commandExecutionContext.setCommandExecutionData(commandExecutionData);
      commandExecutionContext.setEcsServiceSteadyStateTimeout(ecsServiceSteadyStateTimeout);
      return commandExecutionContext;
    }
  }
}
