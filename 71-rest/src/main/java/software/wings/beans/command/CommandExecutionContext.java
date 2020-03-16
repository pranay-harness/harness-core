package software.wings.beans.command;

import static io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability;
import static io.harness.govern.Switch.unhandled;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.service.impl.aws.model.AwsConstants.AWS_SIMPLE_HTTP_CONNECTIVITY_URL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.command.CommandExecutionData;
import io.harness.delegate.task.mixin.AwsRegionCapabilityGenerator;
import io.harness.delegate.task.mixin.IgnoreValidationCapabilityGenerator;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Data;
import software.wings.api.DeploymentType;
import software.wings.beans.AppContainer;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.infrastructure.Host;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.delegatetasks.validation.capabilities.BasicValidationInfo;
import software.wings.delegatetasks.validation.capabilities.SSHHostValidationCapability;
import software.wings.delegatetasks.validation.capabilities.WinrmHostValidationCapability;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by peeyushaggarwal on 6/9/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CommandExecutionContext implements ExecutionCapabilityDemander {
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
  private Map<String, String> serviceVariables = new HashMap<>();
  private Map<String, String> safeDisplayServiceVariables = new HashMap<>();
  private Map<String, String> envVariables = new HashMap<>();
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
  private Map<String, String> metadata = new HashMap<>();
  private CommandExecutionData commandExecutionData;
  private Integer timeout;
  private String deploymentType;
  private List<EncryptedDataDetail> artifactServerEncryptedDataDetails;
  private boolean inlineSshCommand;
  private boolean executeOnDelegate;

  // new fields for multi artifact
  private Map<String, Artifact> multiArtifactMap;
  private Map<String, ArtifactStreamAttributes> artifactStreamAttributesMap;
  private boolean multiArtifact;
  private Map<String, List<EncryptedDataDetail>> artifactServerEncryptedDataDetailsMap;
  private String artifactFileName;

  public CommandExecutionContext() {}

  /**
   * Instantiates a new Command execution context.
   *
   * @param other the other
   */
  public CommandExecutionContext(CommandExecutionContext other) {
    accountId = other.accountId;
    envId = other.envId;
    host = other.host;
    appId = other.appId;
    activityId = other.activityId;
    runtimePath = other.runtimePath;
    stagingPath = other.stagingPath;
    backupPath = other.backupPath;
    windowsRuntimePath = other.windowsRuntimePath;
    serviceTemplateId = other.serviceTemplateId;
    executionCredential = other.executionCredential;
    appContainer = other.appContainer;
    artifactFiles = other.artifactFiles;
    serviceVariables = other.serviceVariables;
    safeDisplayServiceVariables = other.safeDisplayServiceVariables;
    envVariables = other.envVariables;
    hostConnectionAttributes = other.hostConnectionAttributes;
    hostConnectionCredentials = other.hostConnectionCredentials;
    bastionConnectionAttributes = other.bastionConnectionAttributes;
    bastionConnectionCredentials = other.bastionConnectionCredentials;
    winrmConnectionAttributes = other.winrmConnectionAttributes;
    winrmConnectionEncryptedDataDetails = other.winrmConnectionEncryptedDataDetails;
    artifactStreamAttributes = other.artifactStreamAttributes;
    cloudProviderSetting = other.cloudProviderSetting;
    cloudProviderCredentials = other.cloudProviderCredentials;
    codeDeployParams = other.codeDeployParams;
    containerSetupParams = other.containerSetupParams;
    containerResizeParams = other.containerResizeParams;
    metadata = other.metadata;
    commandExecutionData = other.commandExecutionData;
    timeout = other.timeout;
    deploymentType = other.deploymentType;
    artifactServerEncryptedDataDetails = other.artifactServerEncryptedDataDetails;
    inlineSshCommand = other.inlineSshCommand;
    executeOnDelegate = other.executeOnDelegate;
    artifactStreamAttributesMap = other.artifactStreamAttributesMap;
    multiArtifactMap = other.multiArtifactMap;
    multiArtifact = other.multiArtifact;
    artifactServerEncryptedDataDetailsMap = other.artifactServerEncryptedDataDetailsMap;
    artifactFileName = other.artifactFileName;
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

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    String region = null;
    DeploymentType dType = DeploymentType.valueOf(getDeploymentType());
    switch (dType) {
      case KUBERNETES:
        return CapabilityHelper.generateDelegateCapabilities(cloudProviderSetting.getValue(), cloudProviderCredentials);
      case WINRM:
        return singletonList(WinrmHostValidationCapability.builder()
                                 .validationInfo(BasicValidationInfo.builder()
                                                     .accountId(accountId)
                                                     .appId(appId)
                                                     .activityId(activityId)
                                                     .executeOnDelegate(executeOnDelegate)
                                                     .publicDns(host.getPublicDns())
                                                     .build())
                                 .winRmConnectionAttributes(winrmConnectionAttributes)
                                 .winrmConnectionEncryptedDataDetails(winrmConnectionEncryptedDataDetails)
                                 .build());
      case SSH:
        return singletonList(SSHHostValidationCapability.builder()
                                 .validationInfo(BasicValidationInfo.builder()
                                                     .accountId(accountId)
                                                     .appId(appId)
                                                     .activityId(activityId)
                                                     .executeOnDelegate(executeOnDelegate)
                                                     .publicDns(host.getPublicDns())
                                                     .build())
                                 .hostConnectionAttributes(hostConnectionAttributes)
                                 .bastionConnectionAttributes(bastionConnectionAttributes)
                                 .hostConnectionCredentials(hostConnectionCredentials)
                                 .bastionConnectionCredentials(bastionConnectionCredentials)
                                 .sshExecutionCredential((SSHExecutionCredential) executionCredential)
                                 .build());
      case ECS:
        if (containerSetupParams != null) {
          region = ((EcsSetupParams) containerSetupParams).getRegion();
        } else if (containerResizeParams != null) {
          region = ((EcsResizeParams) containerResizeParams).getRegion();
        }
        return singletonList(AwsRegionCapabilityGenerator.buildAwsRegionCapability(region));
      case AWS_CODEDEPLOY:
        return singletonList(buildHttpConnectionExecutionCapability(AWS_SIMPLE_HTTP_CONNECTIVITY_URL));
      case AMI:
      case AWS_LAMBDA:
        return singletonList(IgnoreValidationCapabilityGenerator.buildIgnoreValidationCapability());
      default:
        unhandled(deploymentType);
        throw new WingsException(ErrorCode.INVALID_ARGUMENT)
            .addParam("args", "deploymentType is not handled: " + dType.name());
    }
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
    private Map<String, String> serviceVariables = new HashMap<>();
    private Map<String, String> safeDisplayServiceVariables = new HashMap<>();
    private Map<String, String> envVariables = new HashMap<>();
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
    private Map<String, String> metadata = new HashMap<>();
    private CommandExecutionData commandExecutionData;
    private Integer timeout;
    private String deploymentType;
    private List<EncryptedDataDetail> artifactServerEncryptedDataDetails;
    private boolean inlineSshCommand;
    private boolean executeOnDelegate;

    // new fields for multi artifact
    private Map<String, Artifact> multiArtifactMap;
    private Map<String, ArtifactStreamAttributes> artifactStreamAttributesMap;
    private boolean multiArtifact;
    private Map<String, List<EncryptedDataDetail>> artifactServerEncryptedDataDetailsMap;
    private String artifactFileName;

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
      hostConnectionCredentials = encryptedDataDetails;
      return this;
    }

    public Builder withBastionConnectionAttributes(SettingAttribute bastionConnectionAttributes) {
      this.bastionConnectionAttributes = bastionConnectionAttributes;
      return this;
    }

    public Builder withBastionConnectionCredentials(List<EncryptedDataDetail> encryptedDataDetails) {
      bastionConnectionCredentials = encryptedDataDetails;
      return this;
    }

    public Builder withWinRmConnectionAttributes(WinRmConnectionAttributes winRmConnectionAttributes) {
      winrmConnectionAttributes = winRmConnectionAttributes;
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
      cloudProviderCredentials = encryptedDataDetails;
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

    public Builder withInlineSshCommand(boolean inlineSshCommand) {
      this.inlineSshCommand = inlineSshCommand;
      return this;
    }

    public Builder withExecuteOnDelegate(boolean executeOnDelegate) {
      this.executeOnDelegate = executeOnDelegate;
      return this;
    }

    public Builder withArtifactStreamAttributesMap(Map<String, ArtifactStreamAttributes> artifactStreamAttributesMap) {
      this.artifactStreamAttributesMap = artifactStreamAttributesMap;
      return this;
    }

    public Builder withMultiArtifactMap(Map<String, Artifact> multiArtifactMap) {
      this.multiArtifactMap = multiArtifactMap;
      return this;
    }

    public Builder withMultiArtifact(boolean multiArtifact) {
      this.multiArtifact = multiArtifact;
      return this;
    }

    public Builder withArtifactServerEncryptedDataDetailsMap(
        Map<String, List<EncryptedDataDetail>> artifactServerEncryptedDataDetailsMap) {
      this.artifactServerEncryptedDataDetailsMap = artifactServerEncryptedDataDetailsMap;
      return this;
    }

    public Builder withArtifactFileName(String artifactFileName) {
      this.artifactFileName = artifactFileName;
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
          .withArtifactServerEncryptedDataDetails(artifactServerEncryptedDataDetails)
          .withInlineSshCommand(inlineSshCommand)
          .withExecuteOnDelegate(executeOnDelegate)
          .withArtifactStreamAttributesMap(artifactStreamAttributesMap)
          .withMultiArtifactMap(multiArtifactMap)
          .withMultiArtifact(multiArtifact)
          .withArtifactServerEncryptedDataDetailsMap(artifactServerEncryptedDataDetailsMap)
          .withArtifactFileName(artifactFileName);
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
      commandExecutionContext.setInlineSshCommand(inlineSshCommand);
      commandExecutionContext.setExecuteOnDelegate(executeOnDelegate);
      commandExecutionContext.setArtifactStreamAttributesMap(artifactStreamAttributesMap);
      commandExecutionContext.setMultiArtifactMap(multiArtifactMap);
      commandExecutionContext.setMultiArtifact(multiArtifact);
      commandExecutionContext.setArtifactServerEncryptedDataDetailsMap(artifactServerEncryptedDataDetailsMap);
      commandExecutionContext.setArtifactFileName(artifactFileName);
      return commandExecutionContext;
    }
  }
}
