package software.wings.beans.artifact;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.EmbeddedUser;
import software.wings.utils.Util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by sgurubelli on 6/21/17.
 */
@JsonTypeName("ARTIFACTORY")
public class ArtifactoryArtifactStream extends ArtifactStream {
  private String repositoryType = "any";
  @NotEmpty private String jobname;
  private String groupId;
  private String imageName;
  private List<String> artifactPaths;
  private String artifactPattern;
  private String dockerRepositoryServer;

  public boolean getMetadataOnly() {
    return super.isMetadataOnly();
  }
  public String getRepositoryType() {
    return repositoryType;
  }

  public void setRepositoryType(String repositoryType) {
    this.repositoryType = repositoryType;
  }

  public ArtifactoryArtifactStream() {
    super(ARTIFACTORY.name());
    super.setAutoApproveForProduction(true);
  }

  @Override
  public String getArtifactDisplayName(String buildNo) {
    if (isBlank(getImageName())) {
      return String.format("%s_%s_%s", getSourceName(), buildNo, getDateFormat().format(new Date()));
    }
    return String.format("%s_%s_%s", getJobname() + "/" + getImageName(), buildNo, getDateFormat().format(new Date()));
  }

  /**
   * Get Repository
   *
   * @return the Repository
   */
  public String getJobname() {
    return jobname;
  }

  /**
   * Set repository
   *
   * @param jobname
   */
  public void setJobname(String jobname) {
    this.jobname = jobname;
  }

  /**
   * Get Artifact Pattern
   *
   * @return
   */
  public String getArtifactPattern() {
    return artifactPattern;
  }

  /**
   * Set artifact pattern
   *
   * @param artifactPattern
   */
  public void setArtifactPattern(String artifactPattern) {
    this.artifactPattern = artifactPattern;
  }

  /**
   * Gets artifact paths.
   *
   * @return the artifact paths
   */
  public List<String> getArtifactPaths() {
    return artifactPaths;
  }

  /**
   * Sets artifact paths.
   *
   * @param artifactPaths the artifact paths
   */
  public void setArtifactPaths(List<String> artifactPaths) {
    this.artifactPaths = artifactPaths;
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
   * @return groupId
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * Set Group Id
   */
  public void setGroupId(String groupId) {
    this.groupId = groupId;
    this.imageName = groupId;
  }

  public String getDockerRepositoryServer() {
    return dockerRepositoryServer;
  }

  public void setDockerRepositoryServer(String dockerRepositoryServer) {
    this.dockerRepositoryServer = dockerRepositoryServer;
  }

  @Override
  public String generateName() {
    return Util.normalize(generateSourceName());
  }

  @Override
  public String generateSourceName() {
    StringBuilder builder = new StringBuilder(getJobname());
    builder.append('/');
    if (isBlank(getImageName())) {
      builder.append(getArtifactPattern());
    } else {
      builder.append(getImageName());
    }

    return builder.toString();
  }

  @Override
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withJobName(jobname)
        .withImageName(imageName)
        .withGroupId(getGroupId())
        .withArtifactPattern(artifactPattern)
        .withArtifactName(artifactPaths == null ? "" : artifactPaths.get(0))
        .withRepositoryType(repositoryType)
        .withMetadataOnly(isMetadataOnly())
        .build();
  }

  @Override
  public ArtifactStream clone() {
    return Builder.anArtifactoryArtifactStream()
        .withAppId(getAppId())
        .withSourceName(getSourceName())
        .withSettingId(getSettingId())
        .withAutoApproveForProduction(isAutoApproveForProduction())
        .withJobname(getJobname())
        .withArtifactPaths(getArtifactPaths())
        .withArtifactPattern(getArtifactPattern())
        .withMetadataOnly(getMetadataOnly())
        .withGroupId(getGroupId())
        .withImageName(getImageName())
        .withRepositoryType(getRepositoryType())
        .build();
  }

  /**
   * Clone and return builder.
   *
   * @return the builder
   */
  public Builder deepClone() {
    return ArtifactoryArtifactStream.Builder.anArtifactoryArtifactStream()
        .withJobname(getJobname())
        .withArtifactPaths(getArtifactPaths())
        .withSourceName(getSourceName())
        .withSettingId(getSettingId())
        .withServiceId(getServiceId())
        .withUuid(getUuid())
        .withAppId(getAppId())
        .withCreatedBy(getCreatedBy())
        .withCreatedAt(getCreatedAt())
        .withLastUpdatedBy(getLastUpdatedBy())
        .withLastUpdatedAt(getLastUpdatedAt())
        .withAutoApproveForProduction(isAutoApproveForProduction())
        .withMetadataOnly(getMetadataOnly())
        .withArtifactPattern(getArtifactPattern())
        .withMetadataOnly(getMetadataOnly())
        .withImageName(getImageName())
        .withGroupId(getGroupId());
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String jobname;
    private List<String> artifactPaths;
    private String sourceName;
    private String settingId;
    private String serviceId;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean autoApproveForProduction;
    private boolean metadataOnly;
    private List<ArtifactStreamAction> streamActions = new ArrayList<>();
    private String artifactPattern;
    private String groupId;
    private String imageName;
    private String repositoryType;
    private String dockerRepositoryServer;

    private Builder() {}

    /**
     * A jenkins artifact stream builder.
     *
     * @return the builder
     */
    public static Builder anArtifactoryArtifactStream() {
      return new Builder();
    }

    /**
     * With jobname builder.
     *
     * @param jobname the jobname
     * @return the builder
     */
    public Builder withJobname(String jobname) {
      this.jobname = jobname;
      return this;
    }

    /**
     * With GroupId builder
     *
     * @param groupId the groupId
     */
    public Builder withGroupId(String groupId) {
      this.groupId = groupId;
      return this;
    }

    public Builder withImageName(String imageName) {
      this.imageName = imageName;
      return this;
    }

    /**
     * With artifact paths builder.
     *
     * @param artifactPaths the artifact paths
     * @return the builder
     */
    public Builder withArtifactPaths(List<String> artifactPaths) {
      this.artifactPaths = artifactPaths;
      return this;
    }

    /**
     * With source name builder.
     *
     * @param sourceName the source name
     * @return the builder
     */
    public Builder withSourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    /**
     * With setting id builder.
     *
     * @param settingId the setting id
     * @return the builder
     */
    public Builder withSettingId(String settingId) {
      this.settingId = settingId;
      return this;
    }

    /**
     * With service id builder.
     *
     * @param serviceId the service id
     * @return the builder
     */
    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With auto approve for production builder.
     *
     * @param autoApproveForProduction the auto approve for production
     * @return the builder
     */
    public Builder withAutoApproveForProduction(boolean autoApproveForProduction) {
      this.autoApproveForProduction = autoApproveForProduction;
      return this;
    }

    /**
     * With MetadataOnly
     */
    public Builder withMetadataOnly(boolean metadataOnly) {
      this.metadataOnly = metadataOnly;
      return this;
    }

    /**
     * With stream actions builder.
     *
     * @param streamActions the stream actions
     * @return the builder
     */
    public Builder withStreamActions(List<ArtifactStreamAction> streamActions) {
      this.streamActions = streamActions;
      return this;
    }

    /**
     * With artifactpattern
     *
     * @param artifactPattern
     * @return
     */
    public Builder withArtifactPattern(String artifactPattern) {
      this.artifactPattern = artifactPattern;
      return this;
    }

    /***
     * With repository type
     */
    public Builder withRepositoryType(String repositoryType) {
      this.repositoryType = repositoryType;
      return this;
    }

    public Builder withDockerRepositoryServer(String dockerRepositoryServer) {
      this.dockerRepositoryServer = dockerRepositoryServer;
      return this;
    }
    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anArtifactoryArtifactStream()
          .withJobname(jobname)
          .withArtifactPaths(artifactPaths)
          .withSourceName(sourceName)
          .withSettingId(settingId)
          .withServiceId(serviceId)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withAutoApproveForProduction(autoApproveForProduction)
          .withStreamActions(streamActions)
          .withMetadataOnly(metadataOnly)
          .withArtifactPattern(artifactPattern)
          .withMetadataOnly(metadataOnly)
          .withImageName(imageName)
          .withGroupId(groupId)
          .withRepositoryType(repositoryType)
          .withDockerRepositoryServer(dockerRepositoryServer);
    }

    /**
     * Artifactory Artifact Stream
     */
    public ArtifactoryArtifactStream build() {
      ArtifactoryArtifactStream artifactoryArtifactStream = new ArtifactoryArtifactStream();
      artifactoryArtifactStream.setJobname(jobname);
      artifactoryArtifactStream.setArtifactPaths(artifactPaths);
      artifactoryArtifactStream.setSourceName(sourceName);
      artifactoryArtifactStream.setSettingId(settingId);
      artifactoryArtifactStream.setServiceId(serviceId);
      artifactoryArtifactStream.setUuid(uuid);
      artifactoryArtifactStream.setAppId(appId);
      artifactoryArtifactStream.setCreatedBy(createdBy);
      artifactoryArtifactStream.setCreatedAt(createdAt);
      artifactoryArtifactStream.setLastUpdatedBy(lastUpdatedBy);
      artifactoryArtifactStream.setLastUpdatedAt(lastUpdatedAt);
      artifactoryArtifactStream.setAutoApproveForProduction(autoApproveForProduction);
      artifactoryArtifactStream.setMetadataOnly(metadataOnly);
      artifactoryArtifactStream.setArtifactPattern(artifactPattern);
      artifactoryArtifactStream.setImageName(imageName);
      artifactoryArtifactStream.setRepositoryType(repositoryType);
      return artifactoryArtifactStream;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends ArtifactStream.Yaml {
    private String repositoryName;
    private String groupId;
    private String imageName;
    private List<String> artifactPaths;
    private String artifactPattern;
    private String repositoryType;
    private String dockerRepositoryServer;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String repositoryName,
        String groupId, String imageName, List<String> artifactPaths, String artifactPattern) {
      super(ARTIFACTORY.name(), harnessApiVersion, serverName, metadataOnly);
      this.repositoryName = repositoryName;
      this.groupId = groupId;
      this.imageName = imageName;
      this.artifactPaths = artifactPaths;
      this.artifactPattern = artifactPattern;
    }
  }
}
