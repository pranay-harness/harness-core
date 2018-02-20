package software.wings.beans.artifact;

import static software.wings.beans.artifact.AcrArtifactStream.Builder.anAcrArtifactStream;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;

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

import java.util.Date;

@JsonTypeName("ACR")
public class AcrArtifactStream extends ArtifactStream {
  @UIOrder(4) @NotEmpty @Attributes(title = "Azure Subscription Id", required = true) private String subscriptionId;

  @UIOrder(5) @NotEmpty @Attributes(title = "Container Registry Name", required = true) private String registryName;

  @UIOrder(6) @NotEmpty @Attributes(title = "Repository Name", required = true) private String repositoryName;

  public AcrArtifactStream() {
    super(ACR.name());
    super.setAutoApproveForProduction(true);
    super.setAutoDownload(true);
  }

  @Override
  @SchemaIgnore
  public String getArtifactDisplayName(String buildNo) {
    return String.format("%s_%s_%s", getRepositoryName(), buildNo, getDateFormat().format(new Date()));
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

  @Override
  @SchemaIgnore
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withSubscriptionId(subscriptionId)
        .withRegistryName(registryName)
        .withRepositoryName(repositoryName)
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

  @UIOrder(7)
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
    return new StringBuilder(getRegistryName()).append('/').append(getRepositoryName()).toString();
  }

  @Override
  public ArtifactStream clone() {
    return anAcrArtifactStream()
        .withAppId(getAppId())
        .withSourceName(getSourceName())
        .withSettingId(getSettingId())
        .withAutoApproveForProduction(getAutoApproveForProduction())
        .withSubscriptionId(getSubscriptionId())
        .withRegistryName(getRegistryName())
        .withRepositoryName(getRepositoryName())
        .build();
  }

  /**
   * clone and return builder
   * @return
   */
  public Builder deepClone() {
    return anAcrArtifactStream()
        .withSubscriptionId(getSubscriptionId())
        .withRegistryName(getRegistryName())
        .withRepositoryName(getRepositoryName())
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
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    protected String appId;
    private String subscriptionId;
    private String registryName;
    private String repositoryName;
    private String uuid;
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

    public static Builder anAcrArtifactStream() {
      return new Builder();
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

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
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

    public AcrArtifactStream build() {
      AcrArtifactStream acrArtifactStream = new AcrArtifactStream();
      acrArtifactStream.setSubscriptionId(subscriptionId);
      acrArtifactStream.setRegistryName(registryName);
      acrArtifactStream.setRepositoryName(repositoryName);
      acrArtifactStream.setUuid(uuid);
      acrArtifactStream.setAppId(appId);
      acrArtifactStream.setSourceName(sourceName);
      acrArtifactStream.setCreatedBy(createdBy);
      acrArtifactStream.setSettingId(settingId);
      acrArtifactStream.setCreatedAt(createdAt);
      acrArtifactStream.setLastUpdatedBy(lastUpdatedBy);
      acrArtifactStream.setName(name);
      acrArtifactStream.setLastUpdatedAt(lastUpdatedAt);
      acrArtifactStream.setAutoPopulate(autoPopulate);
      acrArtifactStream.setServiceId(serviceId);
      acrArtifactStream.setEntityYamlPath(entityYamlPath);
      acrArtifactStream.setAutoDownload(autoDownload);
      acrArtifactStream.setAutoApproveForProduction(autoApproveForProduction);
      acrArtifactStream.setMetadataOnly(metadataOnly);
      return acrArtifactStream;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends ArtifactStream.Yaml {
    private String subscriptionId;
    private String registryName;
    private String repositoryName;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String subscriptionId,
        String registryName, String repositoryName) {
      super(ACR.name(), harnessApiVersion, serverName, metadataOnly);
      this.subscriptionId = subscriptionId;
      this.registryName = registryName;
      this.repositoryName = repositoryName;
    }
  }
}
