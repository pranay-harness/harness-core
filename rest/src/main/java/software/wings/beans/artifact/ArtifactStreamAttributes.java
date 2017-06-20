package software.wings.beans.artifact;

import com.google.common.base.MoreObjects;

import software.wings.beans.SettingAttribute;

/**
 * Created by anubhaw on 1/27/17.
 */
public class ArtifactStreamAttributes {
  private String jobName;
  private String imageName;
  private String artifactStreamType;
  private SettingAttribute serverSetting;
  // TODO : Refactoring has to be done
  private String groupId; // For nexus integration
  private String artifactName;

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("jobName", jobName)
        .add("imageName", imageName)
        .add("artifactStreamType", artifactStreamType)
        .add("serverSetting", serverSetting)
        .add("groupId", groupId)
        .add("artifactName", artifactName)
        .toString();
  }

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
   * The type Builder.
   */
  public static final class Builder {
    private String jobName;
    private String imageName;
    private String artifactStreamType;
    private SettingAttribute serverSetting;
    // TODO : Refactoring has to be done
    private String groupId; // For nexus integration
    private String artifactName;

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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anArtifactStreamAttributes()
          .withJobName(jobName)
          .withImageName(imageName)
          .withArtifactStreamType(artifactStreamType)
          .withServerSetting(serverSetting)
          .withGroupId(groupId)
          .withArtifactName(artifactName);
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
      artifactStreamAttributes.setArtifactStreamType(artifactStreamType);
      artifactStreamAttributes.setServerSetting(serverSetting);
      artifactStreamAttributes.setGroupId(groupId);
      artifactStreamAttributes.setArtifactName(artifactName);
      return artifactStreamAttributes;
    }
  }
}
