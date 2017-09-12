package software.wings.beans.command;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import software.wings.api.ContainerServiceData;
import software.wings.beans.AppContainer;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.infrastructure.Host;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by peeyushaggarwal on 6/9/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
  private Map<String, String> envVariables = Maps.newHashMap();
  private SettingAttribute hostConnectionAttributes;
  private SettingAttribute bastionConnectionAttributes;
  private ArtifactStreamAttributes artifactStreamAttributes;
  private SettingAttribute cloudProviderSetting;
  private String clusterName;
  private String serviceName;
  private String region;
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
    this.envVariables = other.envVariables;
    this.hostConnectionAttributes = other.hostConnectionAttributes;
    this.bastionConnectionAttributes = other.bastionConnectionAttributes;
    this.artifactStreamAttributes = other.artifactStreamAttributes;
    this.cloudProviderSetting = other.cloudProviderSetting;
    this.clusterName = other.clusterName;
    this.serviceName = other.serviceName;
    this.region = other.region;
    this.codeDeployParams = other.codeDeployParams;
    this.metadata = other.metadata;
    this.desiredCounts = other.desiredCounts;
    this.commandExecutionData = other.commandExecutionData;
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

  public String getAccountId() {
    return this.accountId;
  }

  public String getEnvId() {
    return this.envId;
  }

  public Host getHost() {
    return this.host;
  }

  public String getAppId() {
    return this.appId;
  }

  public String getActivityId() {
    return this.activityId;
  }

  public String getRuntimePath() {
    return this.runtimePath;
  }

  public String getStagingPath() {
    return this.stagingPath;
  }

  public String getBackupPath() {
    return this.backupPath;
  }

  public String getServiceTemplateId() {
    return this.serviceTemplateId;
  }

  public ExecutionCredential getExecutionCredential() {
    return this.executionCredential;
  }

  public AppContainer getAppContainer() {
    return this.appContainer;
  }

  public List<ArtifactFile> getArtifactFiles() {
    return this.artifactFiles;
  }

  public Map<String, String> getServiceVariables() {
    return this.serviceVariables;
  }

  public Map<String, String> getEnvVariables() {
    return this.envVariables;
  }

  public SettingAttribute getHostConnectionAttributes() {
    return this.hostConnectionAttributes;
  }

  public SettingAttribute getBastionConnectionAttributes() {
    return this.bastionConnectionAttributes;
  }

  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return this.artifactStreamAttributes;
  }

  public SettingAttribute getCloudProviderSetting() {
    return this.cloudProviderSetting;
  }

  public String getClusterName() {
    return this.clusterName;
  }

  public String getServiceName() {
    return this.serviceName;
  }

  public String getRegion() {
    return this.region;
  }

  public CodeDeployParams getCodeDeployParams() {
    return this.codeDeployParams;
  }

  public Map<String, String> getMetadata() {
    return this.metadata;
  }

  public List<ContainerServiceData> getDesiredCounts() {
    return this.desiredCounts;
  }

  public CommandExecutionData getCommandExecutionData() {
    return this.commandExecutionData;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public void setHost(Host host) {
    this.host = host;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public void setRuntimePath(String runtimePath) {
    this.runtimePath = runtimePath;
  }

  public void setStagingPath(String stagingPath) {
    this.stagingPath = stagingPath;
  }

  public void setBackupPath(String backupPath) {
    this.backupPath = backupPath;
  }

  public void setServiceTemplateId(String serviceTemplateId) {
    this.serviceTemplateId = serviceTemplateId;
  }

  public void setExecutionCredential(ExecutionCredential executionCredential) {
    this.executionCredential = executionCredential;
  }

  public void setAppContainer(AppContainer appContainer) {
    this.appContainer = appContainer;
  }

  public void setArtifactFiles(List<ArtifactFile> artifactFiles) {
    this.artifactFiles = artifactFiles;
  }

  public void setServiceVariables(Map<String, String> serviceVariables) {
    this.serviceVariables = serviceVariables;
  }

  public void setEnvVariables(Map<String, String> envVariables) {
    this.envVariables = envVariables;
  }

  public void setHostConnectionAttributes(SettingAttribute hostConnectionAttributes) {
    this.hostConnectionAttributes = hostConnectionAttributes;
  }

  public void setBastionConnectionAttributes(SettingAttribute bastionConnectionAttributes) {
    this.bastionConnectionAttributes = bastionConnectionAttributes;
  }

  public void setArtifactStreamAttributes(ArtifactStreamAttributes artifactStreamAttributes) {
    this.artifactStreamAttributes = artifactStreamAttributes;
  }

  public void setCloudProviderSetting(SettingAttribute cloudProviderSetting) {
    this.cloudProviderSetting = cloudProviderSetting;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public void setCodeDeployParams(CodeDeployParams codeDeployParams) {
    this.codeDeployParams = codeDeployParams;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  public void setDesiredCounts(List<ContainerServiceData> desiredCounts) {
    this.desiredCounts = desiredCounts;
  }

  public void setCommandExecutionData(CommandExecutionData commandExecutionData) {
    this.commandExecutionData = commandExecutionData;
  }

  public boolean equals(Object o) {
    if (o == this)
      return true;
    if (!(o instanceof CommandExecutionContext))
      return false;
    final CommandExecutionContext other = (CommandExecutionContext) o;
    if (!other.canEqual((Object) this))
      return false;
    final Object this$accountId = this.getAccountId();
    final Object other$accountId = other.getAccountId();
    if (this$accountId == null ? other$accountId != null : !this$accountId.equals(other$accountId))
      return false;
    final Object this$envId = this.getEnvId();
    final Object other$envId = other.getEnvId();
    if (this$envId == null ? other$envId != null : !this$envId.equals(other$envId))
      return false;
    final Object this$host = this.getHost();
    final Object other$host = other.getHost();
    if (this$host == null ? other$host != null : !this$host.equals(other$host))
      return false;
    final Object this$appId = this.getAppId();
    final Object other$appId = other.getAppId();
    if (this$appId == null ? other$appId != null : !this$appId.equals(other$appId))
      return false;
    final Object this$activityId = this.getActivityId();
    final Object other$activityId = other.getActivityId();
    if (this$activityId == null ? other$activityId != null : !this$activityId.equals(other$activityId))
      return false;
    final Object this$runtimePath = this.getRuntimePath();
    final Object other$runtimePath = other.getRuntimePath();
    if (this$runtimePath == null ? other$runtimePath != null : !this$runtimePath.equals(other$runtimePath))
      return false;
    final Object this$stagingPath = this.getStagingPath();
    final Object other$stagingPath = other.getStagingPath();
    if (this$stagingPath == null ? other$stagingPath != null : !this$stagingPath.equals(other$stagingPath))
      return false;
    final Object this$backupPath = this.getBackupPath();
    final Object other$backupPath = other.getBackupPath();
    if (this$backupPath == null ? other$backupPath != null : !this$backupPath.equals(other$backupPath))
      return false;
    final Object this$serviceTemplateId = this.getServiceTemplateId();
    final Object other$serviceTemplateId = other.getServiceTemplateId();
    if (this$serviceTemplateId == null ? other$serviceTemplateId != null
                                       : !this$serviceTemplateId.equals(other$serviceTemplateId))
      return false;
    final Object this$executionCredential = this.getExecutionCredential();
    final Object other$executionCredential = other.getExecutionCredential();
    if (this$executionCredential == null ? other$executionCredential != null
                                         : !this$executionCredential.equals(other$executionCredential))
      return false;
    final Object this$appContainer = this.getAppContainer();
    final Object other$appContainer = other.getAppContainer();
    if (this$appContainer == null ? other$appContainer != null : !this$appContainer.equals(other$appContainer))
      return false;
    final Object this$artifactFiles = this.getArtifactFiles();
    final Object other$artifactFiles = other.getArtifactFiles();
    if (this$artifactFiles == null ? other$artifactFiles != null : !this$artifactFiles.equals(other$artifactFiles))
      return false;
    final Object this$serviceVariables = this.getServiceVariables();
    final Object other$serviceVariables = other.getServiceVariables();
    if (this$serviceVariables == null ? other$serviceVariables != null
                                      : !this$serviceVariables.equals(other$serviceVariables))
      return false;
    final Object this$envVariables = this.getEnvVariables();
    final Object other$envVariables = other.getEnvVariables();
    if (this$envVariables == null ? other$envVariables != null : !this$envVariables.equals(other$envVariables))
      return false;
    final Object this$hostConnectionAttributes = this.getHostConnectionAttributes();
    final Object other$hostConnectionAttributes = other.getHostConnectionAttributes();
    if (this$hostConnectionAttributes == null ? other$hostConnectionAttributes != null
                                              : !this$hostConnectionAttributes.equals(other$hostConnectionAttributes))
      return false;
    final Object this$bastionConnectionAttributes = this.getBastionConnectionAttributes();
    final Object other$bastionConnectionAttributes = other.getBastionConnectionAttributes();
    if (this$bastionConnectionAttributes == null
            ? other$bastionConnectionAttributes != null
            : !this$bastionConnectionAttributes.equals(other$bastionConnectionAttributes))
      return false;
    final Object this$artifactStreamAttributes = this.getArtifactStreamAttributes();
    final Object other$artifactStreamAttributes = other.getArtifactStreamAttributes();
    if (this$artifactStreamAttributes == null ? other$artifactStreamAttributes != null
                                              : !this$artifactStreamAttributes.equals(other$artifactStreamAttributes))
      return false;
    final Object this$cloudProviderSetting = this.getCloudProviderSetting();
    final Object other$cloudProviderSetting = other.getCloudProviderSetting();
    if (this$cloudProviderSetting == null ? other$cloudProviderSetting != null
                                          : !this$cloudProviderSetting.equals(other$cloudProviderSetting))
      return false;
    final Object this$clusterName = this.getClusterName();
    final Object other$clusterName = other.getClusterName();
    if (this$clusterName == null ? other$clusterName != null : !this$clusterName.equals(other$clusterName))
      return false;
    final Object this$serviceName = this.getServiceName();
    final Object other$serviceName = other.getServiceName();
    if (this$serviceName == null ? other$serviceName != null : !this$serviceName.equals(other$serviceName))
      return false;
    final Object this$region = this.getRegion();
    final Object other$region = other.getRegion();
    if (this$region == null ? other$region != null : !this$region.equals(other$region))
      return false;
    final Object this$codeDeployParams = this.getCodeDeployParams();
    final Object other$codeDeployParams = other.getCodeDeployParams();
    if (this$codeDeployParams == null ? other$codeDeployParams != null
                                      : !this$codeDeployParams.equals(other$codeDeployParams))
      return false;
    final Object this$metadata = this.getMetadata();
    final Object other$metadata = other.getMetadata();
    if (this$metadata == null ? other$metadata != null : !this$metadata.equals(other$metadata))
      return false;
    final Object this$desiredCounts = this.getDesiredCounts();
    final Object other$desiredCounts = other.getDesiredCounts();
    if (this$desiredCounts == null ? other$desiredCounts != null : !this$desiredCounts.equals(other$desiredCounts))
      return false;
    final Object this$commandExecutionData = this.getCommandExecutionData();
    final Object other$commandExecutionData = other.getCommandExecutionData();
    if (this$commandExecutionData == null ? other$commandExecutionData != null
                                          : !this$commandExecutionData.equals(other$commandExecutionData))
      return false;
    return true;
  }

  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    final Object $accountId = this.getAccountId();
    result = result * PRIME + ($accountId == null ? 43 : $accountId.hashCode());
    final Object $envId = this.getEnvId();
    result = result * PRIME + ($envId == null ? 43 : $envId.hashCode());
    final Object $host = this.getHost();
    result = result * PRIME + ($host == null ? 43 : $host.hashCode());
    final Object $appId = this.getAppId();
    result = result * PRIME + ($appId == null ? 43 : $appId.hashCode());
    final Object $activityId = this.getActivityId();
    result = result * PRIME + ($activityId == null ? 43 : $activityId.hashCode());
    final Object $runtimePath = this.getRuntimePath();
    result = result * PRIME + ($runtimePath == null ? 43 : $runtimePath.hashCode());
    final Object $stagingPath = this.getStagingPath();
    result = result * PRIME + ($stagingPath == null ? 43 : $stagingPath.hashCode());
    final Object $backupPath = this.getBackupPath();
    result = result * PRIME + ($backupPath == null ? 43 : $backupPath.hashCode());
    final Object $serviceTemplateId = this.getServiceTemplateId();
    result = result * PRIME + ($serviceTemplateId == null ? 43 : $serviceTemplateId.hashCode());
    final Object $executionCredential = this.getExecutionCredential();
    result = result * PRIME + ($executionCredential == null ? 43 : $executionCredential.hashCode());
    final Object $appContainer = this.getAppContainer();
    result = result * PRIME + ($appContainer == null ? 43 : $appContainer.hashCode());
    final Object $artifactFiles = this.getArtifactFiles();
    result = result * PRIME + ($artifactFiles == null ? 43 : $artifactFiles.hashCode());
    final Object $serviceVariables = this.getServiceVariables();
    result = result * PRIME + ($serviceVariables == null ? 43 : $serviceVariables.hashCode());
    final Object $envVariables = this.getEnvVariables();
    result = result * PRIME + ($envVariables == null ? 43 : $envVariables.hashCode());
    final Object $hostConnectionAttributes = this.getHostConnectionAttributes();
    result = result * PRIME + ($hostConnectionAttributes == null ? 43 : $hostConnectionAttributes.hashCode());
    final Object $bastionConnectionAttributes = this.getBastionConnectionAttributes();
    result = result * PRIME + ($bastionConnectionAttributes == null ? 43 : $bastionConnectionAttributes.hashCode());
    final Object $artifactStreamAttributes = this.getArtifactStreamAttributes();
    result = result * PRIME + ($artifactStreamAttributes == null ? 43 : $artifactStreamAttributes.hashCode());
    final Object $cloudProviderSetting = this.getCloudProviderSetting();
    result = result * PRIME + ($cloudProviderSetting == null ? 43 : $cloudProviderSetting.hashCode());
    final Object $clusterName = this.getClusterName();
    result = result * PRIME + ($clusterName == null ? 43 : $clusterName.hashCode());
    final Object $serviceName = this.getServiceName();
    result = result * PRIME + ($serviceName == null ? 43 : $serviceName.hashCode());
    final Object $region = this.getRegion();
    result = result * PRIME + ($region == null ? 43 : $region.hashCode());
    final Object $codeDeployParams = this.getCodeDeployParams();
    result = result * PRIME + ($codeDeployParams == null ? 43 : $codeDeployParams.hashCode());
    final Object $metadata = this.getMetadata();
    result = result * PRIME + ($metadata == null ? 43 : $metadata.hashCode());
    final Object $desiredCounts = this.getDesiredCounts();
    result = result * PRIME + ($desiredCounts == null ? 43 : $desiredCounts.hashCode());
    final Object $commandExecutionData = this.getCommandExecutionData();
    result = result * PRIME + ($commandExecutionData == null ? 43 : $commandExecutionData.hashCode());
    return result;
  }

  protected boolean canEqual(Object other) {
    return other instanceof CommandExecutionContext;
  }

  public String toString() {
    return "software.wings.beans.command.CommandExecutionContext(accountId=" + this.getAccountId()
        + ", envId=" + this.getEnvId() + ", host=" + this.getHost() + ", appId=" + this.getAppId() + ", activityId="
        + this.getActivityId() + ", runtimePath=" + this.getRuntimePath() + ", stagingPath=" + this.getStagingPath()
        + ", backupPath=" + this.getBackupPath() + ", serviceTemplateId=" + this.getServiceTemplateId()
        + ", executionCredential=" + this.getExecutionCredential() + ", appContainer=" + this.getAppContainer()
        + ", artifactFiles=" + this.getArtifactFiles() + ", serviceVariables=" + this.getServiceVariables()
        + ", envVariables=" + this.getEnvVariables() + ", hostConnectionAttributes="
        + this.getHostConnectionAttributes() + ", bastionConnectionAttributes=" + this.getBastionConnectionAttributes()
        + ", artifactStreamAttributes=" + this.getArtifactStreamAttributes()
        + ", cloudProviderSetting=" + this.getCloudProviderSetting() + ", clusterName=" + this.getClusterName()
        + ", serviceName=" + this.getServiceName() + ", region=" + this.getRegion() + ", codeDeployParams="
        + this.getCodeDeployParams() + ", metadata=" + this.getMetadata() + ", desiredCounts=" + this.getDesiredCounts()
        + ", commandExecutionData=" + this.getCommandExecutionData() + ")";
  }

  /**
   * The type Code deploy params.
   */
  public static class CodeDeployParams {
    private String applicationName;
    private String deploymentConfigurationName;
    private String deploymentGroupName;
    private String region;
    private String bucket;
    private String key;
    private String bundleType;

    public CodeDeployParams() {}

    private CodeDeployParams(Builder builder) {
      setApplicationName(builder.applicationName);
      setDeploymentConfigurationName(builder.deploymentConfigurationName);
      setDeploymentGroupName(builder.deploymentGroupName);
      setRegion(builder.region);
      setBucket(builder.bucket);
      setKey(builder.key);
      setBundleType(builder.bundleType);
    }

    public String getApplicationName() {
      return this.applicationName;
    }

    public String getDeploymentConfigurationName() {
      return this.deploymentConfigurationName;
    }

    public String getDeploymentGroupName() {
      return this.deploymentGroupName;
    }

    public String getRegion() {
      return this.region;
    }

    public String getBucket() {
      return this.bucket;
    }

    public String getKey() {
      return this.key;
    }

    public String getBundleType() {
      return this.bundleType;
    }

    public void setApplicationName(String applicationName) {
      this.applicationName = applicationName;
    }

    public void setDeploymentConfigurationName(String deploymentConfigurationName) {
      this.deploymentConfigurationName = deploymentConfigurationName;
    }

    public void setDeploymentGroupName(String deploymentGroupName) {
      this.deploymentGroupName = deploymentGroupName;
    }

    public void setRegion(String region) {
      this.region = region;
    }

    public void setBucket(String bucket) {
      this.bucket = bucket;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public void setBundleType(String bundleType) {
      this.bundleType = bundleType;
    }

    public boolean equals(Object o) {
      if (o == this)
        return true;
      if (!(o instanceof CodeDeployParams))
        return false;
      final CodeDeployParams other = (CodeDeployParams) o;
      if (!other.canEqual((Object) this))
        return false;
      final Object this$applicationName = this.getApplicationName();
      final Object other$applicationName = other.getApplicationName();
      if (this$applicationName == null ? other$applicationName != null
                                       : !this$applicationName.equals(other$applicationName))
        return false;
      final Object this$deploymentConfigurationName = this.getDeploymentConfigurationName();
      final Object other$deploymentConfigurationName = other.getDeploymentConfigurationName();
      if (this$deploymentConfigurationName == null
              ? other$deploymentConfigurationName != null
              : !this$deploymentConfigurationName.equals(other$deploymentConfigurationName))
        return false;
      final Object this$deploymentGroupName = this.getDeploymentGroupName();
      final Object other$deploymentGroupName = other.getDeploymentGroupName();
      if (this$deploymentGroupName == null ? other$deploymentGroupName != null
                                           : !this$deploymentGroupName.equals(other$deploymentGroupName))
        return false;
      final Object this$region = this.getRegion();
      final Object other$region = other.getRegion();
      if (this$region == null ? other$region != null : !this$region.equals(other$region))
        return false;
      final Object this$bucket = this.getBucket();
      final Object other$bucket = other.getBucket();
      if (this$bucket == null ? other$bucket != null : !this$bucket.equals(other$bucket))
        return false;
      final Object this$key = this.getKey();
      final Object other$key = other.getKey();
      if (this$key == null ? other$key != null : !this$key.equals(other$key))
        return false;
      final Object this$bundleType = this.getBundleType();
      final Object other$bundleType = other.getBundleType();
      if (this$bundleType == null ? other$bundleType != null : !this$bundleType.equals(other$bundleType))
        return false;
      return true;
    }

    public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      final Object $applicationName = this.getApplicationName();
      result = result * PRIME + ($applicationName == null ? 43 : $applicationName.hashCode());
      final Object $deploymentConfigurationName = this.getDeploymentConfigurationName();
      result = result * PRIME + ($deploymentConfigurationName == null ? 43 : $deploymentConfigurationName.hashCode());
      final Object $deploymentGroupName = this.getDeploymentGroupName();
      result = result * PRIME + ($deploymentGroupName == null ? 43 : $deploymentGroupName.hashCode());
      final Object $region = this.getRegion();
      result = result * PRIME + ($region == null ? 43 : $region.hashCode());
      final Object $bucket = this.getBucket();
      result = result * PRIME + ($bucket == null ? 43 : $bucket.hashCode());
      final Object $key = this.getKey();
      result = result * PRIME + ($key == null ? 43 : $key.hashCode());
      final Object $bundleType = this.getBundleType();
      result = result * PRIME + ($bundleType == null ? 43 : $bundleType.hashCode());
      return result;
    }

    protected boolean canEqual(Object other) {
      return other instanceof CodeDeployParams;
    }

    public String toString() {
      return "software.wings.beans.command.CommandExecutionContext.CodeDeployParams(applicationName="
          + this.getApplicationName() + ", deploymentConfigurationName=" + this.getDeploymentConfigurationName()
          + ", deploymentGroupName=" + this.getDeploymentGroupName() + ", region=" + this.getRegion()
          + ", bucket=" + this.getBucket() + ", key=" + this.getKey() + ", bundleType=" + this.getBundleType() + ")";
    }

    public static final class Builder {
      private String applicationName;
      private String deploymentConfigurationName;
      private String deploymentGroupName;
      private String region;
      private String bucket;
      private String key;
      private String bundleType;

      public Builder() {}

      public Builder withApplicationName(String applicationName) {
        this.applicationName = applicationName;
        return this;
      }

      public Builder withDeploymentConfigurationName(String deploymentConfigurationName) {
        this.deploymentConfigurationName = deploymentConfigurationName;
        return this;
      }

      public Builder withDeploymentGroupName(String deploymentGroupName) {
        this.deploymentGroupName = deploymentGroupName;
        return this;
      }

      public Builder withRegion(String region) {
        this.region = region;
        return this;
      }

      public Builder withBucket(String bucket) {
        this.bucket = bucket;
        return this;
      }

      public Builder withKey(String key) {
        this.key = key;
        return this;
      }

      public Builder withBundleType(String bundleType) {
        this.bundleType = bundleType;
        return this;
      }

      public CodeDeployParams build() {
        return new CodeDeployParams(this);
      }
    }
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
    private Map<String, String> envVariables = Maps.newHashMap();
    private SettingAttribute hostConnectionAttributes;
    private SettingAttribute bastionConnectionAttributes;
    private ArtifactStreamAttributes artifactStreamAttributes;
    private SettingAttribute cloudProviderSetting;
    private String clusterName;
    private String serviceName;
    private String region;
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

    public Builder withEnvVariables(Map<String, String> envVariables) {
      this.envVariables = envVariables;
      return this;
    }

    public Builder withHostConnectionAttributes(SettingAttribute hostConnectionAttributes) {
      this.hostConnectionAttributes = hostConnectionAttributes;
      return this;
    }

    public Builder withBastionConnectionAttributes(SettingAttribute bastionConnectionAttributes) {
      this.bastionConnectionAttributes = bastionConnectionAttributes;
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

    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder withServiceName(String serviceName) {
      this.serviceName = serviceName;
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
          .withArtifactStreamAttributes(artifactStreamAttributes)
          .withCloudProviderSetting(cloudProviderSetting)
          .withClusterName(clusterName)
          .withServiceName(serviceName)
          .withRegion(region)
          .withCodeDeployParams(codeDeployParams)
          .withMetadata(metadata)
          .withDesiredCounts(desiredCounts)
          .withCommandExecutionData(commandExecutionData);
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
      commandExecutionContext.setEnvVariables(envVariables);
      commandExecutionContext.setHostConnectionAttributes(hostConnectionAttributes);
      commandExecutionContext.setBastionConnectionAttributes(bastionConnectionAttributes);
      commandExecutionContext.setArtifactStreamAttributes(artifactStreamAttributes);
      commandExecutionContext.setCloudProviderSetting(cloudProviderSetting);
      commandExecutionContext.setClusterName(clusterName);
      commandExecutionContext.setServiceName(serviceName);
      commandExecutionContext.setRegion(region);
      commandExecutionContext.setCodeDeployParams(codeDeployParams);
      commandExecutionContext.setMetadata(metadata);
      commandExecutionContext.setDesiredCounts(desiredCounts);
      commandExecutionContext.setCommandExecutionData(commandExecutionData);
      return commandExecutionContext;
    }
  }
}
