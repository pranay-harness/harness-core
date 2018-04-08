package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.DockerArtifactStream.Builder.aDockerArtifactStream;

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
 * Created by anubhaw on 1/5/17.
 */
@JsonTypeName("DOCKER")
public class DockerArtifactStream extends ArtifactStream {
  @NotEmpty private String imageName;

  /**
   * Instantiates a new Docker artifact stream.
   */
  public DockerArtifactStream() {
    super(DOCKER.name());
    super.setAutoApproveForProduction(true);
    super.setAutoDownload(true);
  }

  @Override
  public String getArtifactDisplayName(String buildNo) {
    return String.format("%s_%s_%s", getImageName(), buildNo, getDateFormat().format(new Date()));
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

  @Override
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withImageName(imageName)
        .build();
  }

  @Override
  public String generateName() {
    return Util.normalize(generateSourceName());
  }

  @Override
  public String generateSourceName() {
    return getImageName();
  }

  @Override
  public ArtifactStream clone() {
    return aDockerArtifactStream()
        .withAppId(getAppId())
        .withSourceName(getSourceName())
        .withSettingId(getSettingId())
        .withAutoApproveForProduction(isAutoApproveForProduction())
        .withImageName(getImageName())
        .build();
  }

  /**
   * clone and return builder
   * @return
   */
  public Builder deepClone() {
    return DockerArtifactStream.Builder.aDockerArtifactStream()
        .withImageName(getImageName())
        .withSourceName(getSourceName())
        .withSettingId(getSettingId())
        .withServiceId(getServiceId())
        .withUuid(getUuid())
        .withAppId(getAppId())
        .withCreatedBy(getCreatedBy())
        .withCreatedAt(getCreatedAt())
        .withLastUpdatedBy(getLastUpdatedBy())
        .withLastUpdatedAt(getLastUpdatedAt())
        .withAutoDownload(isAutoDownload())
        .withAutoApproveForProduction(isAutoApproveForProduction());
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String imageName;
    private String sourceName;
    private String settingId;
    private String serviceId;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean autoDownload;
    private boolean autoApproveForProduction;
    private List<ArtifactStreamAction> streamActions = new ArrayList<>();

    private Builder() {}

    /**
     * A docker artifact stream builder.
     *
     * @return the builder
     */
    public static Builder aDockerArtifactStream() {
      return new Builder();
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
     * With auto download builder.
     *
     * @param autoDownload the auto download
     * @return the builder
     */
    public Builder withAutoDownload(boolean autoDownload) {
      this.autoDownload = autoDownload;
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aDockerArtifactStream()
          .withImageName(imageName)
          .withSourceName(sourceName)
          .withSettingId(settingId)
          .withServiceId(serviceId)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withAutoDownload(autoDownload)
          .withAutoApproveForProduction(autoApproveForProduction)
          .withStreamActions(streamActions);
    }

    /**
     * Build docker artifact stream.
     *
     * @return the docker artifact stream
     */
    public DockerArtifactStream build() {
      DockerArtifactStream dockerArtifactStream = new DockerArtifactStream();
      dockerArtifactStream.setImageName(imageName);
      dockerArtifactStream.setSourceName(sourceName);
      dockerArtifactStream.setSettingId(settingId);
      dockerArtifactStream.setServiceId(serviceId);
      dockerArtifactStream.setUuid(uuid);
      dockerArtifactStream.setAppId(appId);
      dockerArtifactStream.setCreatedBy(createdBy);
      dockerArtifactStream.setCreatedAt(createdAt);
      dockerArtifactStream.setLastUpdatedBy(lastUpdatedBy);
      dockerArtifactStream.setLastUpdatedAt(lastUpdatedAt);
      dockerArtifactStream.setAutoDownload(autoDownload);
      dockerArtifactStream.setAutoApproveForProduction(autoApproveForProduction);
      return dockerArtifactStream;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends ArtifactStream.Yaml {
    private String imageName;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String imageName) {
      super(DOCKER.name(), harnessApiVersion, serverName, metadataOnly);
      this.imageName = imageName;
    }
  }
}
