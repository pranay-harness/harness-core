package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORYDOCKER;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.EmbeddedUser;
import software.wings.stencils.UIOrder;
import software.wings.utils.Util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by sgurubelli on 6/21/17.
 */
@JsonTypeName("ARTIFACTORYDOCKER")
public class ArtifactoryDockerArtifactStream extends ArtifactStream {
  @UIOrder(4) @NotEmpty @Attributes(title = "Repository", required = true) private String jobname;

  @UIOrder(5) @NotEmpty @Attributes(title = "Docker Image Name", required = true) private String groupId;

  @SchemaIgnore private String imageName;

  public ArtifactoryDockerArtifactStream() {
    super(ArtifactStreamType.ARTIFACTORY.name());
    super.setAutoApproveForProduction(true);
    super.setAutoDownload(true);
  }

  @SchemaIgnore
  @Override
  public String getArtifactDisplayName(String buildNo) {
    return String.format("%s_%s_%s", getImageName(), buildNo, getDateFormat().format(new Date()));
  }

  /**
   * Get Repository
   * @return the Repository
   */
  public String getJobname() {
    return jobname;
  }

  /**
   * Set repository
   * @param jobname
   */
  public void setJobname(String jobname) {
    this.jobname = jobname;
  }
  /**
   * Gets image name.
   *
   * @return the image name
   */
  @SchemaIgnore
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

  @SchemaIgnore
  @Override
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withJobName(jobname)
        .withImageName(imageName)
        .build();
  }

  @Attributes(title = "Source Type")
  @Override
  public String getArtifactStreamType() {
    return super.getArtifactStreamType();
  }

  @Attributes(title = "Source Server")
  @Override
  public String getSettingId() {
    return super.getSettingId();
  }

  @UIOrder(5)
  @Attributes(title = "Auto-approved for Production")
  public boolean getAutoApproveForProduction() {
    return super.isAutoApproveForProduction();
  }

  @Override
  public String generateName() {
    return Util.normalize(generateSourceName());
  }

  @Override
  public String generateSourceName() {
    return new StringBuilder(getJobname()).append('/').append(getGroupId()).toString();
  }

  @Override
  public ArtifactStream clone() {
    return ArtifactoryDockerArtifactStream.Builder.anArtifactoryDockerArtifactStream()
        .withAppId(getAppId())
        .withSourceName(getSourceName())
        .withSettingId(getSettingId())
        .withServiceId(getServiceId())
        .withAutoApproveForProduction(getAutoApproveForProduction())
        .withJobname(getJobname())
        .build();
  }

  /**
   * Clone and return builder.
   *
   * @return the builder
   */
  public ArtifactoryDockerArtifactStream.Builder deepClone() {
    return ArtifactoryDockerArtifactStream.Builder.anArtifactoryDockerArtifactStream()
        .withJobname(getJobname())
        .withSourceName(getSourceName())
        .withImageName(getImageName())
        .withSettingId(getSettingId())
        .withServiceId(getServiceId())
        .withUuid(getUuid())
        .withAppId(getAppId())
        .withCreatedBy(getCreatedBy())
        .withCreatedAt(getCreatedAt())
        .withLastUpdatedBy(getLastUpdatedBy())
        .withLastUpdatedAt(getLastUpdatedAt())
        .withAutoApproveForProduction(isAutoApproveForProduction())
        .withMetadataOnly(isMetadataOnly());
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String groupId;
    private String imageName;
    private String jobname;
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

    private Builder() {}

    /**
     * A jenkins artifact stream builder.
     *
     * @return the builder
     */
    public static Builder anArtifactoryDockerArtifactStream() {
      return new ArtifactoryDockerArtifactStream.Builder();
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
     * With imageName builder.
     *
     * @param imageName the imageName
     * @return the builder
     */
    public Builder withImageName(String imageName) {
      this.imageName = imageName;
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
    public ArtifactoryDockerArtifactStream.Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public ArtifactoryDockerArtifactStream.Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public ArtifactoryDockerArtifactStream.Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public ArtifactoryDockerArtifactStream.Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public ArtifactoryDockerArtifactStream.Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With auto approve for production builder.
     *
     * @param autoApproveForProduction the auto approve for production
     * @return the builder
     */
    public ArtifactoryDockerArtifactStream.Builder withAutoApproveForProduction(boolean autoApproveForProduction) {
      this.autoApproveForProduction = autoApproveForProduction;
      return this;
    }

    /**
     *
     */
    public ArtifactoryDockerArtifactStream.Builder withMetadataOnly(boolean metadataOnly) {
      this.metadataOnly = metadataOnly;
      return this;
    }

    /**
     * With stream actions builder.
     *
     * @param streamActions the stream actions
     * @return the builder
     */
    public ArtifactoryDockerArtifactStream.Builder withStreamActions(List<ArtifactStreamAction> streamActions) {
      this.streamActions = streamActions;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public ArtifactoryDockerArtifactStream.Builder but() {
      return anArtifactoryDockerArtifactStream()
          .withJobname(jobname)
          .withSourceName(sourceName)
          .withImageName(imageName)
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
          .withMetadataOnly(metadataOnly);
    }

    /**
     * Artifactory Artifact Stream
     */
    public ArtifactoryDockerArtifactStream build() {
      ArtifactoryDockerArtifactStream artifactoryDockerArtifactStream = new ArtifactoryDockerArtifactStream();
      artifactoryDockerArtifactStream.setJobname(jobname);
      artifactoryDockerArtifactStream.setGroupId(groupId);
      artifactoryDockerArtifactStream.setSourceName(sourceName);
      artifactoryDockerArtifactStream.setSettingId(settingId);
      artifactoryDockerArtifactStream.setServiceId(serviceId);
      artifactoryDockerArtifactStream.setUuid(uuid);
      artifactoryDockerArtifactStream.setAppId(appId);
      artifactoryDockerArtifactStream.setCreatedBy(createdBy);
      artifactoryDockerArtifactStream.setCreatedAt(createdAt);
      artifactoryDockerArtifactStream.setLastUpdatedBy(lastUpdatedBy);
      artifactoryDockerArtifactStream.setLastUpdatedAt(lastUpdatedAt);
      artifactoryDockerArtifactStream.setAutoApproveForProduction(autoApproveForProduction);
      artifactoryDockerArtifactStream.setMetadataOnly(metadataOnly);
      artifactoryDockerArtifactStream.setImageName(imageName);
      return artifactoryDockerArtifactStream;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends ArtifactStream.Yaml {
    private String repositoryName;
    private String dockerImageName;
    private String imageName;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String repositoryName,
        String dockerImageName, String imageName) {
      super(ARTIFACTORYDOCKER.name(), harnessApiVersion, serverName, metadataOnly);
      this.repositoryName = repositoryName;
      this.dockerImageName = dockerImageName;
      this.imageName = imageName;
    }
  }
}
