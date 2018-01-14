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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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
    return new StringBuilder(getRegistryHostName()).append('/').append(getDockerImageName()).toString();
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

  public static final class Builder {
    private static DateFormat dateFormat = new SimpleDateFormat("HHMMSS");
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    protected String appId;
    private String registryHostName;
    private String dockerImageName;
    private String uuid;
    private String artifactStreamType;
    private String sourceName;
    private EmbeddedUser createdBy;
    private String settingId;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private String name;
    private long lastUpdatedAt;
    // auto populate name
    private boolean autoPopulate = true;
    private String serviceId;
    private boolean autoDownload = true;
    private boolean autoApproveForProduction;
    private boolean metadataOnly;

    private Builder() {}

    public static Builder aGcrArtifactStream() {
      return new Builder();
    }

    public Builder withRegistryHostName(String registryHostName) {
      this.registryHostName = registryHostName;
      return this;
    }

    public Builder withDockerImageName(String dockerImageName) {
      this.dockerImageName = dockerImageName;
      return this;
    }

    public Builder withDateFormat(DateFormat dateFormat) {
      this.dateFormat = dateFormat;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withArtifactStreamType(String artifactStreamType) {
      this.artifactStreamType = artifactStreamType;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withSourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withSettingId(String settingId) {
      this.settingId = settingId;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withAutoPopulate(boolean autoPopulate) {
      this.autoPopulate = autoPopulate;
      return this;
    }

    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Builder withEntityYamlPath(String entityYamlPath) {
      this.entityYamlPath = entityYamlPath;
      return this;
    }

    public Builder withAutoDownload(boolean autoDownload) {
      this.autoDownload = autoDownload;
      return this;
    }

    public Builder withAutoApproveForProduction(boolean autoApproveForProduction) {
      this.autoApproveForProduction = autoApproveForProduction;
      return this;
    }

    public Builder withMetadataOnly(boolean metadataOnly) {
      this.metadataOnly = metadataOnly;
      return this;
    }

    public GcrArtifactStream build() {
      GcrArtifactStream gcrArtifactStream = new GcrArtifactStream();
      gcrArtifactStream.setRegistryHostName(registryHostName);
      gcrArtifactStream.setDockerImageName(dockerImageName);
      gcrArtifactStream.setUuid(uuid);
      gcrArtifactStream.setAppId(appId);
      gcrArtifactStream.setSourceName(sourceName);
      gcrArtifactStream.setCreatedBy(createdBy);
      gcrArtifactStream.setSettingId(settingId);
      gcrArtifactStream.setCreatedAt(createdAt);
      gcrArtifactStream.setLastUpdatedBy(lastUpdatedBy);
      gcrArtifactStream.setName(name);
      gcrArtifactStream.setLastUpdatedAt(lastUpdatedAt);
      gcrArtifactStream.setAutoPopulate(autoPopulate);
      gcrArtifactStream.setServiceId(serviceId);
      gcrArtifactStream.setEntityYamlPath(entityYamlPath);
      gcrArtifactStream.setAutoDownload(autoDownload);
      gcrArtifactStream.setAutoApproveForProduction(autoApproveForProduction);
      gcrArtifactStream.setMetadataOnly(metadataOnly);
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
