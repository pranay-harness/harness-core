package software.wings.beans.command;

import static io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability;
import static io.harness.govern.Switch.unhandled;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.service.impl.aws.model.AwsConstants.AWS_SIMPLE_HTTP_CONNECTIVITY_URL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.command.CommandExecutionData;
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
      case AWS_CODEDEPLOY:
        return singletonList(buildHttpConnectionExecutionCapability(AWS_SIMPLE_HTTP_CONNECTIVITY_URL));
      case ECS:
      case AMI:
      case AWS_LAMBDA:
        return emptyList();
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

    public Builder accountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder envId(String envId) {
      this.envId = envId;
      return this;
    }

    public Builder host(Host host) {
      this.host = host;
      return this;
    }

    public Builder appId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder activityId(String activityId) {
      this.activityId = activityId;
      return this;
    }

    public Builder runtimePath(String runtimePath) {
      this.runtimePath = runtimePath;
      return this;
    }

    public Builder stagingPath(String stagingPath) {
      this.stagingPath = stagingPath;
      return this;
    }

    public Builder backupPath(String backupPath) {
      this.backupPath = backupPath;
      return this;
    }

    public Builder windowsRuntimePath(String windowsRuntimePath) {
      this.windowsRuntimePath = windowsRuntimePath;
      return this;
    }

    public Builder serviceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    public Builder executionCredential(ExecutionCredential executionCredential) {
      this.executionCredential = executionCredential;
      return this;
    }

    public Builder appContainer(AppContainer appContainer) {
      this.appContainer = appContainer;
      return this;
    }

    public Builder artifactFiles(List<ArtifactFile> artifactFiles) {
      this.artifactFiles = artifactFiles;
      return this;
    }

    public Builder serviceVariables(Map<String, String> serviceVariables) {
      this.serviceVariables = serviceVariables;
      return this;
    }

    public Builder safeDisplayServiceVariables(Map<String, String> safeDisplayServiceVariables) {
      this.safeDisplayServiceVariables = safeDisplayServiceVariables;
      return this;
    }

    public Builder envVariables(Map<String, String> envVariables) {
      this.envVariables = envVariables;
      return this;
    }

    public Builder hostConnectionAttributes(SettingAttribute hostConnectionAttributes) {
      this.hostConnectionAttributes = hostConnectionAttributes;
      return this;
    }

    public Builder hostConnectionCredentials(List<EncryptedDataDetail> encryptedDataDetails) {
      hostConnectionCredentials = encryptedDataDetails;
      return this;
    }

    public Builder bastionConnectionAttributes(SettingAttribute bastionConnectionAttributes) {
      this.bastionConnectionAttributes = bastionConnectionAttributes;
      return this;
    }

    public Builder bastionConnectionCredentials(List<EncryptedDataDetail> encryptedDataDetails) {
      bastionConnectionCredentials = encryptedDataDetails;
      return this;
    }

    public Builder winRmConnectionAttributes(WinRmConnectionAttributes winRmConnectionAttributes) {
      winrmConnectionAttributes = winRmConnectionAttributes;
      return this;
    }

    public Builder winrmConnectionEncryptedDataDetails(List<EncryptedDataDetail> winrmConnectionEncryptedDataDetails) {
      this.winrmConnectionEncryptedDataDetails = winrmConnectionEncryptedDataDetails;
      return this;
    }

    public Builder artifactStreamAttributes(ArtifactStreamAttributes artifactStreamAttributes) {
      this.artifactStreamAttributes = artifactStreamAttributes;
      return this;
    }

    public Builder cloudProviderSetting(SettingAttribute cloudProviderSetting) {
      this.cloudProviderSetting = cloudProviderSetting;
      return this;
    }

    public Builder cloudProviderCredentials(List<EncryptedDataDetail> encryptedDataDetails) {
      cloudProviderCredentials = encryptedDataDetails;
      return this;
    }

    public Builder serviceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder codeDeployParams(CodeDeployParams codeDeployParams) {
      this.codeDeployParams = codeDeployParams;
      return this;
    }

    public Builder containerSetupParams(ContainerSetupParams containerSetupParams) {
      this.containerSetupParams = containerSetupParams;
      return this;
    }

    public Builder containerResizeParams(ContainerResizeParams containerResizeParams) {
      this.containerResizeParams = containerResizeParams;
      return this;
    }

    public Builder metadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder commandExecutionData(CommandExecutionData commandExecutionData) {
      this.commandExecutionData = commandExecutionData;
      return this;
    }

    public Builder timeout(Integer timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder deploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public Builder artifactServerEncryptedDataDetails(List<EncryptedDataDetail> artifactServerEncryptedDataDetails) {
      this.artifactServerEncryptedDataDetails = artifactServerEncryptedDataDetails;
      return this;
    }

    public Builder inlineSshCommand(boolean inlineSshCommand) {
      this.inlineSshCommand = inlineSshCommand;
      return this;
    }

    public Builder executeOnDelegate(boolean executeOnDelegate) {
      this.executeOnDelegate = executeOnDelegate;
      return this;
    }

    public Builder artifactStreamAttributesMap(Map<String, ArtifactStreamAttributes> artifactStreamAttributesMap) {
      this.artifactStreamAttributesMap = artifactStreamAttributesMap;
      return this;
    }

    public Builder multiArtifactMap(Map<String, Artifact> multiArtifactMap) {
      this.multiArtifactMap = multiArtifactMap;
      return this;
    }

    public Builder multiArtifact(boolean multiArtifact) {
      this.multiArtifact = multiArtifact;
      return this;
    }

    public Builder artifactServerEncryptedDataDetailsMap(
        Map<String, List<EncryptedDataDetail>> artifactServerEncryptedDataDetailsMap) {
      this.artifactServerEncryptedDataDetailsMap = artifactServerEncryptedDataDetailsMap;
      return this;
    }

    public Builder artifactFileName(String artifactFileName) {
      this.artifactFileName = artifactFileName;
      return this;
    }

    public Builder but() {
      return aCommandExecutionContext()
          .accountId(accountId)
          .envId(envId)
          .host(host)
          .appId(appId)
          .activityId(activityId)
          .runtimePath(runtimePath)
          .stagingPath(stagingPath)
          .backupPath(backupPath)
          .windowsRuntimePath(windowsRuntimePath)
          .serviceTemplateId(serviceTemplateId)
          .executionCredential(executionCredential)
          .appContainer(appContainer)
          .artifactFiles(artifactFiles)
          .serviceVariables(serviceVariables)
          .envVariables(envVariables)
          .hostConnectionAttributes(hostConnectionAttributes)
          .bastionConnectionAttributes(bastionConnectionAttributes)
          .hostConnectionCredentials(hostConnectionCredentials)
          .bastionConnectionCredentials(bastionConnectionCredentials)
          .winRmConnectionAttributes(winrmConnectionAttributes)
          .winrmConnectionEncryptedDataDetails(winrmConnectionEncryptedDataDetails)
          .artifactStreamAttributes(artifactStreamAttributes)
          .cloudProviderSetting(cloudProviderSetting)
          .cloudProviderCredentials(cloudProviderCredentials)
          .codeDeployParams(codeDeployParams)
          .metadata(metadata)
          .commandExecutionData(commandExecutionData)
          .containerSetupParams(containerSetupParams)
          .containerResizeParams(containerResizeParams)
          .safeDisplayServiceVariables(safeDisplayServiceVariables)
          .serviceName(serviceName)
          .timeout(timeout)
          .deploymentType(deploymentType)
          .artifactServerEncryptedDataDetails(artifactServerEncryptedDataDetails)
          .inlineSshCommand(inlineSshCommand)
          .executeOnDelegate(executeOnDelegate)
          .artifactStreamAttributesMap(artifactStreamAttributesMap)
          .multiArtifactMap(multiArtifactMap)
          .multiArtifact(multiArtifact)
          .artifactServerEncryptedDataDetailsMap(artifactServerEncryptedDataDetailsMap)
          .artifactFileName(artifactFileName);
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
