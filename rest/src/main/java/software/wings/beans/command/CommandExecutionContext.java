package software.wings.beans.command;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import software.wings.beans.AppContainer;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.infrastructure.Host;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.Collections;
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
  private String windowsRuntimePath;
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
  private WinRmConnectionAttributes winrmConnectionAttributes;
  private List<EncryptedDataDetail> winrmConnectionEncryptedDataDetails;
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
  private List<EncryptedDataDetail> artifactServerEncryptedDataDetails;

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
    this.windowsRuntimePath = other.windowsRuntimePath;
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
    this.winrmConnectionAttributes = other.winrmConnectionAttributes;
    this.winrmConnectionEncryptedDataDetails = other.winrmConnectionEncryptedDataDetails;
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
    this.artifactServerEncryptedDataDetails = other.artifactServerEncryptedDataDetails;
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

  public WinRmSessionConfig winrmSessionConfig(String commandUnitName, String commandPath) {
    return WinRmSessionConfig.builder()
        .accountId(accountId)
        .appId(appId)
        .executionId(activityId)
        .commandUnitName(commandUnitName)
        .hostname(host.getPublicDns())
        .authenticationScheme(winrmConnectionAttributes.getAuthenticationScheme())
        .domain(winrmConnectionAttributes.getDomain())
        .username(winrmConnectionAttributes.getUsername())
        .password(String.valueOf(winrmConnectionAttributes.getPassword()))
        .port(winrmConnectionAttributes.getPort())
        .useSSL(winrmConnectionAttributes.isUseSSL())
        .skipCertChecks(winrmConnectionAttributes.isSkipCertChecks())
        .workingDirectory(commandPath)
        .environment(envVariables == null ? Collections.emptyMap() : envVariables)
        .build();
  }

  public static final class Builder {
    private String accountId;
    private String envId;
    private Host host;
    private String appId;
    private String activityId;
    private String serviceName;
    private String runtimePath;
    private String stagingPath;
    private String backupPath;
    private String windowsRuntimePath;
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
    private WinRmConnectionAttributes winrmConnectionAttributes;
    private List<EncryptedDataDetail> winrmConnectionEncryptedDataDetails;
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
    private List<EncryptedDataDetail> artifactServerEncryptedDataDetails;

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

    public Builder withWindowsRuntimePath(String windowsRuntimePath) {
      this.windowsRuntimePath = windowsRuntimePath;
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

    public Builder withWinRmConnectionAttributes(WinRmConnectionAttributes winRmConnectionAttributes) {
      this.winrmConnectionAttributes = winRmConnectionAttributes;
      return this;
    }

    public Builder withWinrmConnectionEncryptedDataDetails(
        List<EncryptedDataDetail> winrmConnectionEncryptedDataDetails) {
      this.winrmConnectionEncryptedDataDetails = winrmConnectionEncryptedDataDetails;
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

    public Builder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder withCodeDeployParams(CodeDeployParams codeDeployParams) {
      this.codeDeployParams = codeDeployParams;
      return this;
    }

    public Builder withContainerSetupParams(ContainerSetupParams containerSetupParams) {
      this.containerSetupParams = containerSetupParams;
      return this;
    }

    public Builder withContainerResizeParams(ContainerResizeParams containerResizeParams) {
      this.containerResizeParams = containerResizeParams;
      return this;
    }

    public Builder withMetadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder withCommandExecutionData(CommandExecutionData commandExecutionData) {
      this.commandExecutionData = commandExecutionData;
      return this;
    }

    public Builder withTimeout(Integer timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder withDeploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public Builder withArtifactServerEncryptedDataDetails(
        List<EncryptedDataDetail> artifactServerEncryptedDataDetails) {
      this.artifactServerEncryptedDataDetails = artifactServerEncryptedDataDetails;
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
          .withWindowsRuntimePath(windowsRuntimePath)
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
          .withWinRmConnectionAttributes(winrmConnectionAttributes)
          .withWinrmConnectionEncryptedDataDetails(winrmConnectionEncryptedDataDetails)
          .withArtifactStreamAttributes(artifactStreamAttributes)
          .withCloudProviderSetting(cloudProviderSetting)
          .withCloudProviderCredentials(cloudProviderCredentials)
          .withCodeDeployParams(codeDeployParams)
          .withMetadata(metadata)
          .withCommandExecutionData(commandExecutionData)
          .withContainerSetupParams(containerSetupParams)
          .withContainerResizeParams(containerResizeParams)
          .withSafeDisplayServiceVariables(safeDisplayServiceVariables)
          .withServiceName(serviceName)
          .withTimeout(timeout)
          .withDeploymentType(deploymentType)
          .withArtifactServerEncryptedDataDetails(artifactServerEncryptedDataDetails);
    }

    public CommandExecutionContext build() {
      CommandExecutionContext commandExecutionContext = new CommandExecutionContext();
      commandExecutionContext.setAccountId(accountId);
      commandExecutionContext.setEnvId(envId);
      commandExecutionContext.setHost(host);
      commandExecutionContext.setAppId(appId);
      commandExecutionContext.setActivityId(activityId);
      commandExecutionContext.setServiceName(serviceName);
      commandExecutionContext.setRuntimePath(runtimePath);
      commandExecutionContext.setStagingPath(stagingPath);
      commandExecutionContext.setBackupPath(backupPath);
      commandExecutionContext.setWindowsRuntimePath(windowsRuntimePath);
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
      commandExecutionContext.setWinrmConnectionAttributes(winrmConnectionAttributes);
      commandExecutionContext.setWinrmConnectionEncryptedDataDetails(winrmConnectionEncryptedDataDetails);
      commandExecutionContext.setArtifactStreamAttributes(artifactStreamAttributes);
      commandExecutionContext.setCloudProviderSetting(cloudProviderSetting);
      commandExecutionContext.setCloudProviderCredentials(cloudProviderCredentials);
      commandExecutionContext.setCodeDeployParams(codeDeployParams);
      commandExecutionContext.setMetadata(metadata);
      commandExecutionContext.setCommandExecutionData(commandExecutionData);
      commandExecutionContext.setContainerSetupParams(containerSetupParams);
      commandExecutionContext.setContainerResizeParams(containerResizeParams);
      commandExecutionContext.setTimeout(timeout);
      commandExecutionContext.setDeploymentType(deploymentType);
      commandExecutionContext.setArtifactServerEncryptedDataDetails(artifactServerEncryptedDataDetails);
      return commandExecutionContext;
    }
  }
}
