package software.wings.beans.artifact;

import com.google.common.collect.Maps;

import software.wings.beans.SettingAttribute;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 1/27/17.
 */
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
    public Builder withJobName(String jobName) {
      this.jobName = jobName;
      return this;
    }

    /**
     * With image name builder.
     *
     * @param imageName the image name
     * @return the builder
     */
    public Builder withImageName(String imageName) {
      this.imageName = imageName;
      return this;
    }

    public Builder withSubscriptionId(String subscriptionId) {
      this.subscriptionId = subscriptionId;
      return this;
    }

    public Builder withRegistryName(String registryName) {
      this.registryName = registryName;
      return this;
    }

    public Builder withRepositoryName(String repositoryName) {
      this.repositoryName = repositoryName;
      return this;
    }

    /**
     * With registry url builder.
     *
     * @param registryHostName registry host name
     * @return the builder
     */
    public Builder withRegistryHostName(String registryHostName) {
      this.registryHostName = registryHostName;
      return this;
    }

    /**
     * With artifact stream type builder.
     *
     * @param artifactStreamType the artifact stream type
     * @return the builder
     */
    public Builder withArtifactStreamType(String artifactStreamType) {
      this.artifactStreamType = artifactStreamType;
      return this;
    }

    /**
     * With server setting builder.
     *
     * @param serverSetting the server setting
     * @return the builder
     */
    public Builder withServerSetting(SettingAttribute serverSetting) {
      this.serverSetting = serverSetting;
      return this;
    }

    /**
     * With group id
     * @param groupId
     * @return
     */
    public Builder withGroupId(String groupId) {
      this.groupId = groupId;
      return this;
    }

    public Builder withArtifactStreamId(String artifactStreamId) {
      this.artifactStreamId = artifactStreamId;
      return this;
    }

    /**
     * with artifactName
     * @param artifactName
     * @return
     */
    public Builder withArtifactName(String artifactName) {
      this.artifactName = artifactName;
      return this;
    }

    /**
     * With artifact type
     * @param artifactType
     * @return
     */
    public Builder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    public Builder withArtifactPattern(String artifactPattern) {
      this.artifactPattern = artifactPattern;
      return this;
    }

    public Builder withRegion(String region) {
      this.region = region;
      return this;
    }

    public Builder withRepositoryType(String repositoryType) {
      this.repositoryType = repositoryType;
      return this;
    }

    public Builder withMetadataOnly(boolean metadataOnly) {
      this.metadataOnly = metadataOnly;
      return this;
    }

    public Builder withTags(Map<String, List<String>> tags) {
      this.tags = tags;
      return this;
    }

    public Builder withPlatform(String platform) {
      this.platform = platform;
      return this;
    }

    public Builder withFilters(Map<String, String> filters) {
      this.filters = filters;
      return this;
    }

    public Builder withArtifactServerEncryptedDataDetails(
        List<EncryptedDataDetail> artifactServerEncryptedDataDetails) {
      this.artifactServerEncryptedDataDetails = artifactServerEncryptedDataDetails;
      return this;
    }

    public Builder withMetadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder withCopyArtifactEnabledForArtifactory(boolean copyArtifactEnabledForArtifactory) {
      this.copyArtifactEnabledForArtifactory = copyArtifactEnabledForArtifactory;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anArtifactStreamAttributes()
          .withJobName(jobName)
          .withImageName(imageName)
          .withSubscriptionId(subscriptionId)
          .withRegistryName(registryName)
          .withRepositoryName(repositoryName)
          .withRegistryHostName(registryHostName)
          .withArtifactStreamType(artifactStreamType)
          .withServerSetting(serverSetting)
          .withGroupId(groupId)
          .withArtifactStreamId(artifactStreamId)
          .withArtifactName(artifactName)
          .withArtifactType(artifactType)
          .withArtifactPattern(artifactPattern)
          .withRegion(region)
          .withRepositoryType(repositoryType)
          .withMetadataOnly(metadataOnly)
          .withTags(tags)
          .withPlatform(platform)
          .withFilters(filters)
          .withArtifactServerEncryptedDataDetails(artifactServerEncryptedDataDetails)
          .withMetadata(metadata)
          .withCopyArtifactEnabledForArtifactory(copyArtifactEnabledForArtifactory);
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
      return artifactStreamAttributes;
    }
  }
}
