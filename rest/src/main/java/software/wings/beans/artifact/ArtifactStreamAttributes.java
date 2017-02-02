package software.wings.beans.artifact;

import software.wings.beans.SettingAttribute;

/**
 * Created by anubhaw on 1/27/17.
 */
public class ArtifactStreamAttributes {
  private String jobName;
  private String imageName;
  private String artifactStreamType;
  private SettingAttribute serverSetting;

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
   * The type Builder.
   */
  public static final class Builder {
    private String jobName;
    private String imageName;
    private String artifactStreamType;
    private SettingAttribute serverSetting;

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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anArtifactStreamAttributes()
          .withJobName(jobName)
          .withImageName(imageName)
          .withArtifactStreamType(artifactStreamType)
          .withServerSetting(serverSetting);
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
      return artifactStreamAttributes;
    }
  }
}
