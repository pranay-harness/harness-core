package software.wings.beans.artifact;

import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.wings.beans.SettingAttribute;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;

@ToString(exclude = {"serverSetting", "artifactServerEncryptedDataDetails"})
public class ArtifactStreamAttributes {
  private String jobName;
  private String imageName;
  private String registryHostName;
  private String subscriptionId;
  private String registryName;
  private String repositoryName;
  private String artifactStreamType;
  private SettingAttribute serverSetting;
  // TODO : Refactoring has to be done
  private String groupId; // For nexus integration
  private String artifactStreamId;
  private String artifactName;
  private ArtifactType artifactType;
  private String artifactPattern;
  private String region;
  private String repositoryType;
  private boolean metadataOnly;
  private Map<String, List<String>> tags;
  private String platform;
  private Map<String, String> filters;
  private List<EncryptedDataDetail> artifactServerEncryptedDataDetails;
  private Map<String, String> metadata = Maps.newHashMap();
  private boolean copyArtifactEnabledForArtifactory;
  private String artifactoryDockerRepositoryServer;
  private String nexusDockerPort;

  @Getter @Setter private String customScriptTimeout;
  @Getter @Setter private String accountId;
  @Getter @Setter private String customArtifactStreamScript;
  @Getter @Setter private String artifactRoot;
  @Getter @Setter private String buildNoPath;
  @Getter @Setter private Map<String, String> artifactAttributes;
  @Getter @Setter private boolean customAttributeMappingNeeded;

  /**
   * Gets job name.
   *
   * @return the job name
   */
  public String getJobName() {
    return jobName;
  }

  /**
   * Sets job name.
   *
   * @param jobName the job name
   */
  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  /**
   * Gets image name.
   *
   * @return the image name
   */
  public String getImageName() {
    return imageName;
  }

  public void setImageName(String imageName) {
    this.imageName = imageName;
  }

  public String getSubscriptionId() {
    return subscriptionId;
  }

  public void setSubscriptionId(String subscriptionId) {
    this.subscriptionId = subscriptionId;
  }

  public String getRegistryName() {
    return registryName;
  }

  public void setRegistryName(String registryName) {
    this.registryName = registryName;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  /**
   * Gets registry url
   * @return registry url
   */
  public String getRegistryHostName() {
    return registryHostName;
  }

  /**
   * Sets registry url
   * @param registryHostName registry url
   */
  public void setRegistryHostName(String registryHostName) {
    this.registryHostName = registryHostName;
  }

  /**
   * Gets artifact stream type.
   *
   * @return the artifact stream type
   */
  public String getArtifactStreamType() {
    return artifactStreamType;
  }

  /**
   * Sets artifact stream type.
   *
   * @param artifactStreamType the artifact stream type
   */
  public void setArtifactStreamType(String artifactStreamType) {
    this.artifactStreamType = artifactStreamType;
  }

  /**
   * Gets setting attribute.
   *
   * @return the setting attribute
   */
  public SettingAttribute getServerSetting() {
    return serverSetting;
  }

  /**
   * Sets setting attribute.
   *
   * @param serverSetting the setting attribute
   */
  public void setServerSetting(SettingAttribute serverSetting) {
    this.serverSetting = serverSetting;
  }

  /**
   * Gets group Id
   * @return
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * Set group Id
   * @param groupId
   */
  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  /**
   * Gets artifact id
   * @return
   */
  public String getArtifactStreamId() {
    return artifactStreamId;
  }

  public void setArtifactStreamId(String artifactStreamId) {
    this.artifactStreamId = artifactStreamId;
  }

  /**
   * Get Artifact Name
   * @return
   */
  public String getArtifactName() {
    return artifactName;
  }

  /**
   * Set Artifact Name
   * @param artifactName
   */
  public void setArtifactName(String artifactName) {
    this.artifactName = artifactName;
  }

  /**
   * Get Artifact type
   * @return
   */
  public ArtifactType getArtifactType() {
    return artifactType;
  }

  /**
   * Set artifact type
   * @param artifactType
   */
  public void setArtifactType(ArtifactType artifactType) {
    this.artifactType = artifactType;
  }

  /**
   * Get Artifact Pattern
   * @return
   */
  public String getArtifactPattern() {
    return artifactPattern;
  }

  /**
   * Set artifact pattern
   * @param artifactPattern
   */
  public void setArtifactPattern(String artifactPattern) {
    this.artifactPattern = artifactPattern;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getRepositoryType() {
    return repositoryType;
  }

  public void setRepositoryType(String repositoryType) {
    this.repositoryType = repositoryType;
  }

  public void setMedatadataOnly(boolean metadataOnly) {
    this.metadataOnly = metadataOnly;
  }

  public boolean isMetadataOnly() {
    return metadataOnly;
  }

  public Map<String, List<String>> getTags() {
    return tags;
  }

  public void setTags(Map<String, List<String>> tags) {
    this.tags = tags;
  }

  public String getPlatform() {
    return platform;
  }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

  public Map<String, String> getFilters() {
    return filters;
  }

  public void setFilters(Map<String, String> filters) {
    this.filters = filters;
  }

  public List<EncryptedDataDetail> getArtifactServerEncryptedDataDetails() {
    return artifactServerEncryptedDataDetails;
  }

  public void setArtifactServerEncryptedDataDetails(List<EncryptedDataDetail> artifactServerEncryptedDataDetails) {
    this.artifactServerEncryptedDataDetails = artifactServerEncryptedDataDetails;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  public boolean isCopyArtifactEnabledForArtifactory() {
    return copyArtifactEnabledForArtifactory;
  }

  public void setCopyArtifactEnabledForArtifactory(boolean copyArtifactEnabledForArtifactory) {
    this.copyArtifactEnabledForArtifactory = copyArtifactEnabledForArtifactory;
  }

  public String getArtifactoryDockerRepositoryServer() {
    return artifactoryDockerRepositoryServer;
  }

  public void setArtifactoryDockerRepositoryServer(String artifactoryDockerRepositoryServer) {
    this.artifactoryDockerRepositoryServer = artifactoryDockerRepositoryServer;
  }

  public String getNexusDockerPort() {
    return nexusDockerPort;
  }

  public void setNexusDockerPort(String nexusDockerPort) {
    this.nexusDockerPort = nexusDockerPort;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String jobName;
    private String registryHostName;
    private String subscriptionId;
    private String registryName;
    private String repositoryName;
    private String imageName;
    private String artifactStreamType;
    private SettingAttribute serverSetting;
    private String groupId; // For nexus integration
    private String artifactStreamId;
    private String artifactName;
    private ArtifactType artifactType;
    private String artifactPattern;
    private String region;
    private String repositoryType;
    private boolean metadataOnly;
    private Map<String, List<String>> tags;
    private Map<String, String> filters;
    private String platform;
    private List<EncryptedDataDetail> artifactServerEncryptedDataDetails;
    private Map<String, String> metadata = Maps.newHashMap();
    private boolean copyArtifactEnabledForArtifactory;
    private String artifactoryDockerRepositoryServer;
    private String nexusDockerPort;
    private String customScriptTimeout;
    private String accountId;
    private String customArtifactStreamScript;
    private String lastCollectedAt;
    private String artifactRoot;
    private String buildNoPath;
    private Map<String, String> artifactAttributes;
    private boolean customAttributeMappingNeeded;

    private Builder() {}

    /**
     * An artifact stream attributes builder.
     *
     * @return the builder
     */
    public static Builder anArtifactStreamAttributes() {
      return new Builder();
    }

    /**
     * With job name builder.
     *
     * @param jobName the job name
     * @return the builder
     */
    public Builder jobName(String jobName) {
      this.jobName = jobName;
      return this;
    }

    /**
     * With image name builder.
     *
     * @param imageName the image name
     * @return the builder
     */
    public Builder imageName(String imageName) {
      this.imageName = imageName;
      return this;
    }

    public Builder subscriptionId(String subscriptionId) {
      this.subscriptionId = subscriptionId;
      return this;
    }

    public Builder registryName(String registryName) {
      this.registryName = registryName;
      return this;
    }

    public Builder repositoryName(String repositoryName) {
      this.repositoryName = repositoryName;
      return this;
    }

    /**
     * With registry url builder.
     *
     * @param registryHostName registry host name
     * @return the builder
     */
    public Builder registryHostName(String registryHostName) {
      this.registryHostName = registryHostName;
      return this;
    }

    /**
     * With artifact stream type builder.
     *
     * @param artifactStreamType the artifact stream type
     * @return the builder
     */
    public Builder artifactStreamType(String artifactStreamType) {
      this.artifactStreamType = artifactStreamType;
      return this;
    }

    /**
     * With server setting builder.
     *
     * @param serverSetting the server setting
     * @return the builder
     */
    public Builder serverSetting(SettingAttribute serverSetting) {
      this.serverSetting = serverSetting;
      return this;
    }

    /**
     * With group id
     * @param groupId
     * @return
     */
    public Builder groupId(String groupId) {
      this.groupId = groupId;
      return this;
    }

    public Builder artifactStreamId(String artifactStreamId) {
      this.artifactStreamId = artifactStreamId;
      return this;
    }

    /**
     * with artifactName
     * @param artifactName
     * @return
     */
    public Builder artifactName(String artifactName) {
      this.artifactName = artifactName;
      return this;
    }

    /**
     * With artifact type
     * @param artifactType
     * @return
     */
    public Builder artifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    public Builder artifactPattern(String artifactPattern) {
      this.artifactPattern = artifactPattern;
      return this;
    }

    public Builder region(String region) {
      this.region = region;
      return this;
    }

    public Builder repositoryType(String repositoryType) {
      this.repositoryType = repositoryType;
      return this;
    }

    public Builder metadataOnly(boolean metadataOnly) {
      this.metadataOnly = metadataOnly;
      return this;
    }

    public Builder tags(Map<String, List<String>> tags) {
      this.tags = tags;
      return this;
    }

    public Builder platform(String platform) {
      this.platform = platform;
      return this;
    }

    public Builder filters(Map<String, String> filters) {
      this.filters = filters;
      return this;
    }

    public Builder artifactServerEncryptedDataDetails(List<EncryptedDataDetail> artifactServerEncryptedDataDetails) {
      this.artifactServerEncryptedDataDetails = artifactServerEncryptedDataDetails;
      return this;
    }

    public Builder metadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder copyArtifactEnabledForArtifactory(boolean copyArtifactEnabledForArtifactory) {
      this.copyArtifactEnabledForArtifactory = copyArtifactEnabledForArtifactory;
      return this;
    }

    public Builder artifactoryDockerRepositoryServer(String artifactoryDockerRepositoryServer) {
      this.artifactoryDockerRepositoryServer = artifactoryDockerRepositoryServer;
      return this;
    }

    public Builder nexusDockerPort(String nexusDockerPort) {
      this.nexusDockerPort = nexusDockerPort;
      return this;
    }

    public Builder customScriptTimeout(String customScriptTimeout) {
      this.customScriptTimeout = customScriptTimeout;
      return this;
    }

    public Builder customArtifactStreamScript(String customArtifactStreamScript) {
      this.customArtifactStreamScript = customArtifactStreamScript;
      return this;
    }

    public Builder accountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder artifactRoot(String artifactRoot) {
      this.artifactRoot = artifactRoot;
      return this;
    }

    public Builder buildNoPath(String buildNoPath) {
      this.buildNoPath = buildNoPath;
      return this;
    }

    public Builder artifactAttributes(Map<String, String> artifactAttributes) {
      this.artifactAttributes = artifactAttributes;
      return this;
    }

    public Builder customAttributeMappingNeeded(boolean customAttributeMappingNeeded) {
      this.customAttributeMappingNeeded = customAttributeMappingNeeded;
      return this;
    }

    /**
     * Build artifact stream attributes.
     *
     * @return the artifact stream attributes
     */
    public ArtifactStreamAttributes build() {
      ArtifactStreamAttributes artifactStreamAttributes = new ArtifactStreamAttributes();
      artifactStreamAttributes.setJobName(jobName);
      artifactStreamAttributes.setImageName(imageName);
      artifactStreamAttributes.setSubscriptionId(subscriptionId);
      artifactStreamAttributes.setRegistryName(registryName);
      artifactStreamAttributes.setRepositoryName(repositoryName);
      artifactStreamAttributes.setRegistryHostName(registryHostName);
      artifactStreamAttributes.setArtifactStreamType(artifactStreamType);
      artifactStreamAttributes.setServerSetting(serverSetting);
      artifactStreamAttributes.setGroupId(groupId);
      artifactStreamAttributes.setArtifactStreamId(artifactStreamId);
      artifactStreamAttributes.setArtifactName(artifactName);
      artifactStreamAttributes.setArtifactType(artifactType);
      artifactStreamAttributes.setArtifactPattern(artifactPattern);
      artifactStreamAttributes.setRegion(region);
      artifactStreamAttributes.setRepositoryType(repositoryType);
      artifactStreamAttributes.setMedatadataOnly(metadataOnly);
      artifactStreamAttributes.setTags(tags);
      artifactStreamAttributes.setPlatform(platform);
      artifactStreamAttributes.setFilters(filters);
      artifactStreamAttributes.setArtifactServerEncryptedDataDetails(artifactServerEncryptedDataDetails);
      artifactStreamAttributes.setMetadata(metadata);
      artifactStreamAttributes.setCopyArtifactEnabledForArtifactory(copyArtifactEnabledForArtifactory);
      artifactStreamAttributes.setArtifactoryDockerRepositoryServer(artifactoryDockerRepositoryServer);
      artifactStreamAttributes.setNexusDockerPort(nexusDockerPort);
      artifactStreamAttributes.setCustomScriptTimeout(customScriptTimeout);
      artifactStreamAttributes.setCustomArtifactStreamScript(customArtifactStreamScript);
      artifactStreamAttributes.setArtifactRoot(artifactRoot);
      artifactStreamAttributes.setBuildNoPath(buildNoPath);
      artifactStreamAttributes.setArtifactAttributes(artifactAttributes);
      artifactStreamAttributes.setCustomAttributeMappingNeeded(customAttributeMappingNeeded);
      return artifactStreamAttributes;
    }
  }
}
