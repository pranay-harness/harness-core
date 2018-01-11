package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.GcrArtifactStream.Builder.aGcrArtifactStream;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.EmbeddedUser;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.UIOrder;
import software.wings.utils.Util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author rktummala on 8/4/17.
 */
@JsonTypeName("GCR")
public class GcrArtifactStream extends ArtifactStream {
  @UIOrder(4)
  @NotEmpty
  @DefaultValue("gcr.io")
  @Attributes(title = "Registry Host Name", required = true)
  private String registryHostName;

  @UIOrder(5) @NotEmpty @Attributes(title = "Docker Image Name", required = true) private String dockerImageName;

  /**
   * Instantiates a new Docker artifact stream.
   */
  public GcrArtifactStream() {
    super(GCR.name());
    super.setAutoApproveForProduction(true);
    super.setAutoDownload(true);
  }

  @Override
  @SchemaIgnore
  public String getArtifactDisplayName(String buildNo) {
    return String.format("%s_%s_%s", getDockerImageName(), buildNo, getDateFormat().format(new Date()));
  }

  /**
   * Gets image name.
   *
   * @return the image name
   */
  public String getDockerImageName() {
    return dockerImageName;
  }

  /**
   * Sets image name.
   *
   * @param dockerImageName the image name
   */
  public void setDockerImageName(String dockerImageName) {
    this.dockerImageName = dockerImageName;
  }

  @Override
  @SchemaIgnore
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withImageName(dockerImageName)
        .withRegistryHostName(registryHostName)
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

  public String getRegistryHostName() {
    return registryHostName;
  }

  public void setRegistryHostName(String registryHostName) {
    this.registryHostName = registryHostName;
  }

  @Override
  public String generateName() {
    return Util.normalize(generateSourceName());
  }

  @Override
  public String generateSourceName() {
    StringBuilder builder = new StringBuilder(getRegistryHostName());
    builder.append("/");
    builder.append(getDockerImageName());
    return builder.toString();
  }

  @Override
  public ArtifactStream clone() {
    return aGcrArtifactStream()
        .withAppId(getAppId())
        .withSourceName(getSourceName())
        .withSettingId(getSettingId())
        .withAutoApproveForProduction(getAutoApproveForProduction())
        .withDockerImageName(getDockerImageName())
        .withRegistryHostName(getRegistryHostName())
        .build();
  }

  /**
   * clone and return builder
   * @return
   */
  public Builder deepClone() {
    return aGcrArtifactStream()
        .withDockerImageName(getDockerImageName())
        .withRegistryHostName(getRegistryHostName())
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
    private String dockerImageName;
    private String registryHostName;
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
    public static Builder aGcrArtifactStream() {
      return new Builder();
    }

    /**
     * With registryHostName builder.
     *
     * @param registryHostName registry host name. For example, us.gcr.io
     * @return the builder
     */
    public Builder withRegistryHostName(String registryHostName) {
      this.registryHostName = registryHostName;
      return this;
    }

    /**
     * With image name builder.
     *
     * @param imageName the image name
     * @return the builder
     */
    public Builder withDockerImageName(String imageName) {
      this.dockerImageName = imageName;
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
      return aGcrArtifactStream()
          .withDockerImageName(dockerImageName)
          .withRegistryHostName(registryHostName)
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
    /**
     * Build docker artifact stream.
     *
     * @return the gcr artifact stream
     */
    public GcrArtifactStream build() {
      GcrArtifactStream gcrArtifactStream = new GcrArtifactStream();
      gcrArtifactStream.setDockerImageName(dockerImageName);
      gcrArtifactStream.setRegistryHostName(registryHostName);
      gcrArtifactStream.setSourceName(sourceName);
      gcrArtifactStream.setSettingId(settingId);
      gcrArtifactStream.setServiceId(serviceId);
      gcrArtifactStream.setUuid(uuid);
      gcrArtifactStream.setAppId(appId);
      gcrArtifactStream.setCreatedBy(createdBy);
      gcrArtifactStream.setCreatedAt(createdAt);
      gcrArtifactStream.setLastUpdatedBy(lastUpdatedBy);
      gcrArtifactStream.setLastUpdatedAt(lastUpdatedAt);
      gcrArtifactStream.setAutoDownload(autoDownload);
      gcrArtifactStream.setAutoApproveForProduction(autoApproveForProduction);
      return gcrArtifactStream;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends ArtifactStream.Yaml {
    private String registryHostName;
    private String dockerImageName;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String registryHostName,
        String dockerImageName) {
      super(GCR.name(), harnessApiVersion, serverName, metadataOnly);
      this.registryHostName = registryHostName;
      this.dockerImageName = dockerImageName;
    }
  }
}
