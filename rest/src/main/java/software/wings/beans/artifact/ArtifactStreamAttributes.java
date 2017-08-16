package software.wings.beans.artifact;

import software.wings.beans.SettingAttribute;
import software.wings.utils.ArtifactType;

/**
 * Created by anubhaw on 1/27/17.
 */
public class ArtifactStreamAttributes {
  private String jobName;
  private String imageName;
  private String registryHostName;
  private String artifactStreamType;
  private SettingAttribute serverSetting;
  // TODO : Refactoring has to be done
  private String groupId; // For nexus integration
  private String artifactName;
  private ArtifactType artifactType;
  private String artifactPattern;
  private String region;

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

  /**
   * Sets image name.
   *
   * @param imageName the image name
   */
  public void setImageName(String imageName) {
    this.imageName = imageName;
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

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String jobName;
    private String registryHostName;
    private String imageName;
    private String artifactStreamType;
    private SettingAttribute serverSetting;
    private String groupId; // For nexus integration
    private String artifactName;
    private ArtifactType artifactType;
    private String artifactPattern;
    private String region;

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

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anArtifactStreamAttributes()
          .withJobName(jobName)
          .withImageName(imageName)
          .withRegistryHostName(registryHostName)
          .withArtifactStreamType(artifactStreamType)
          .withServerSetting(serverSetting)
          .withGroupId(groupId)
          .withArtifactName(artifactName)
          .withArtifactType(artifactType)
          .withArtifactPattern(artifactPattern)
          .withRegion(region);
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
      artifactStreamAttributes.setRegistryHostName(registryHostName);
      artifactStreamAttributes.setArtifactStreamType(artifactStreamType);
      artifactStreamAttributes.setServerSetting(serverSetting);
      artifactStreamAttributes.setGroupId(groupId);
      artifactStreamAttributes.setArtifactName(artifactName);
      artifactStreamAttributes.setArtifactType(artifactType);
      artifactStreamAttributes.setArtifactPattern(artifactPattern);
      artifactStreamAttributes.setRegion(region);
      return artifactStreamAttributes;
    }
  }
}
