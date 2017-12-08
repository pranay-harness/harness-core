package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang.StringUtils;
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
@JsonTypeName("ARTIFACTORY")
public class ArtifactoryArtifactStream extends ArtifactStream {
  private String repositoryType = "any";

  @UIOrder(4) @NotEmpty @Attributes(title = "Repository", required = true) private String jobname;

  @SchemaIgnore private String groupId;

  @SchemaIgnore private String imageName;

  @SchemaIgnore @Attributes(title = "Artifact Path") private List<String> artifactPaths;

  @UIOrder(5) @Attributes(title = "Artifact Path / File Filter") private String artifactPattern;

  @UIOrder(6)
  @Attributes(title = "Meta-data Only (Artifact download not required)")
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
    super(ArtifactStreamType.ARTIFACTORY.name());
    super.setAutoApproveForProduction(true);
  }

  @SchemaIgnore
  @Override
  public String getArtifactDisplayName(String buildNo) {
    if (StringUtils.isBlank(getImageName())) {
      return String.format("%s_%s_%s", getSourceName(), buildNo, getDateFormat().format(new Date()));
    }
    return String.format("%s_%s_%s", getJobname() + "/" + getImageName(), buildNo, getDateFormat().format(new Date()));
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
    StringBuilder builder = new StringBuilder(getJobname());
    builder.append("/");
    if (StringUtils.isBlank(getImageName())) {
      builder.append(getArtifactPattern());
    } else {
      builder.append(getImageName());
    }

    return builder.toString();
  }

  @SchemaIgnore
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
        .withAutoApproveForProduction(getAutoApproveForProduction())
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
        .withAutoApproveForProduction(getAutoApproveForProduction())
        .withStreamActions(getStreamActions())
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
    private boolean autoApproveForProduction = false;
    private boolean metadataOnly = false;
    private List<ArtifactStreamAction> streamActions = new ArrayList<>();
    private String artifactPattern;
    private String groupId;
    private String imageName;
    private String repositoryType;

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

    /**
     * But builder.
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
          .withRepositoryType(repositoryType);
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
      artifactoryArtifactStream.setStreamActions(streamActions);
      artifactoryArtifactStream.setMetadataOnly(metadataOnly);
      artifactoryArtifactStream.setArtifactPattern(artifactPattern);
      artifactoryArtifactStream.setImageName(imageName);
      artifactoryArtifactStream.setRepositoryType(repositoryType);
      return artifactoryArtifactStream;
    }
  }
  @Data
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends ArtifactStream.Yaml {
    private String repositoryName;
    private String groupId;
    private String imageName;
    private List<String> artifactPaths;
    private String artifactPattern;

    public static final class Builder {
      private String repositoryName;
      private String sourceName;
      private String groupId;
      private String settingName;
      private String imageName;
      private boolean autoApproveForProduction = false;
      private List<String> artifactPaths;
      private String type;
      private String artifactPattern;
      private boolean metadataOnly = false;

      private Builder() {}

      public static Yaml.Builder aYaml() {
        return new Yaml.Builder();
      }

      public Yaml.Builder withRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
        return this;
      }

      public Yaml.Builder withSourceName(String sourceName) {
        this.sourceName = sourceName;
        return this;
      }

      public Yaml.Builder withGroupId(String groupId) {
        this.groupId = groupId;
        return this;
      }

      public Yaml.Builder withSettingName(String settingName) {
        this.settingName = settingName;
        return this;
      }

      public Yaml.Builder withImageName(String imageName) {
        this.imageName = imageName;
        return this;
      }

      public Yaml.Builder withAutoApproveForProduction(boolean autoApproveForProduction) {
        this.autoApproveForProduction = autoApproveForProduction;
        return this;
      }

      public Yaml.Builder withArtifactPaths(List<String> artifactPaths) {
        this.artifactPaths = artifactPaths;
        return this;
      }

      public Yaml.Builder withType(String type) {
        this.type = type;
        return this;
      }

      public Yaml.Builder withArtifactPattern(String artifactPattern) {
        this.artifactPattern = artifactPattern;
        return this;
      }

      public Yaml.Builder withMetadataOnly(boolean metadataOnly) {
        this.metadataOnly = metadataOnly;
        return this;
      }

      public Yaml.Builder but() {
        return aYaml()
            .withRepositoryName(repositoryName)
            .withSourceName(sourceName)
            .withGroupId(groupId)
            .withSettingName(settingName)
            .withImageName(imageName)
            .withAutoApproveForProduction(autoApproveForProduction)
            .withArtifactPaths(artifactPaths)
            .withType(type)
            .withArtifactPattern(artifactPattern)
            .withMetadataOnly(metadataOnly);
      }

      public Yaml build() {
        Yaml yaml = new Yaml();
        yaml.setRepositoryName(repositoryName);
        yaml.setSourceName(sourceName);
        yaml.setGroupId(groupId);
        yaml.setSettingName(settingName);
        yaml.setImageName(imageName);
        yaml.setAutoApproveForProduction(autoApproveForProduction);
        yaml.setArtifactPaths(artifactPaths);
        yaml.setType(type);
        yaml.setArtifactPattern(artifactPattern);
        yaml.setMetadataOnly(metadataOnly);
        return yaml;
      }
    }
  }
}
