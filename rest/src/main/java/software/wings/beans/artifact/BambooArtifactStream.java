package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.BAMBOO;
import static software.wings.beans.artifact.BambooArtifactStream.Builder.aBambooArtifactStream;

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
 * Created by anubhaw on 11/22/16.
 */
@JsonTypeName("BAMBOO")
public class BambooArtifactStream extends ArtifactStream {
  @UIOrder(4) @NotEmpty @Attributes(title = "Plan Name", required = true) private String jobname;

  @UIOrder(5)
  @Attributes(title = "Meta-data Only (Artifact download not required)")
  public boolean getMetadataOnly() {
    return super.isMetadataOnly();
  }

  @UIOrder(6) @NotEmpty @Attributes(title = "Artifact Path", required = true) private List<String> artifactPaths;

  /**
   * Instantiates a new Bamboo artifact stream.
   */
  public BambooArtifactStream() {
    super(BAMBOO.name());
    super.setAutoApproveForProduction(true);
  }

  @Override
  @SchemaIgnore
  public String getArtifactDisplayName(String buildNo) {
    return String.format("%s_%s_%s", getSourceName(), buildNo, getDateFormat().format(new Date()));
  }

  /**
   * Gets jobname.
   *
   * @return the jobname
   */
  public String getJobname() {
    return jobname;
  }

  /**
   * Sets jobname.
   *
   * @param jobname the jobname
   */
  public void setJobname(String jobname) {
    this.jobname = jobname;
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

  @Override
  public String generateName() {
    return Util.normalize(generateSourceName());
  }

  @Override
  public String generateSourceName() {
    return getJobname();
  }

  @UIOrder(6)
  @Attributes(title = "Auto-approved for Production")
  public boolean getAutoApproveForProduction() {
    return super.isAutoApproveForProduction();
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

  @Override
  @SchemaIgnore
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return anArtifactStreamAttributes().withArtifactStreamType(getArtifactStreamType()).withJobName(jobname).build();
  }

  @Override
  public ArtifactStream clone() {
    return aBambooArtifactStream()
        .withAppId(getAppId())
        .withSourceName(getSourceName())
        .withSettingId(getSettingId())
        .withServiceId(getServiceId())
        .withAutoApproveForProduction(getAutoApproveForProduction())
        .withJobname(getJobname())
        .withArtifactPaths(getArtifactPaths())
        .build();
  }

  /**
   * clone and return builder
   * @return
   */
  public BambooArtifactStream.Builder deepClone() {
    return aBambooArtifactStream()
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
        .withMetadataOnly(isMetadataOnly());
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
    private List<ArtifactStreamAction> streamActions = new ArrayList<>();
    private boolean metadataOnly;

    private Builder() {}

    /**
     * A bamboo artifact stream builder.
     *
     * @return the builder
     */
    public static Builder aBambooArtifactStream() {
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
     * With MetadataOnly builder.
     */
    public Builder withMetadataOnly(boolean metadataOnly) {
      this.metadataOnly = metadataOnly;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aBambooArtifactStream()
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
          .withMetadataOnly(metadataOnly);
    }

    /**
     * Build bamboo artifact stream.
     *
     * @return the bamboo artifact stream
     */
    public BambooArtifactStream build() {
      BambooArtifactStream bambooArtifactStream = new BambooArtifactStream();
      bambooArtifactStream.setJobname(jobname);
      bambooArtifactStream.setArtifactPaths(artifactPaths);
      bambooArtifactStream.setSourceName(sourceName);
      bambooArtifactStream.setSettingId(settingId);
      bambooArtifactStream.setServiceId(serviceId);
      bambooArtifactStream.setUuid(uuid);
      bambooArtifactStream.setAppId(appId);
      bambooArtifactStream.setCreatedBy(createdBy);
      bambooArtifactStream.setCreatedAt(createdAt);
      bambooArtifactStream.setLastUpdatedBy(lastUpdatedBy);
      bambooArtifactStream.setLastUpdatedAt(lastUpdatedAt);
      bambooArtifactStream.setAutoApproveForProduction(autoApproveForProduction);
      bambooArtifactStream.setMetadataOnly(metadataOnly);
      return bambooArtifactStream;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends ArtifactStream.Yaml {
    private String planName;
    private List<String> artifactPaths;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String planName,
        List<String> artifactPaths) {
      super(BAMBOO.name(), harnessApiVersion, serverName, metadataOnly);
      this.planName = planName;
      this.artifactPaths = artifactPaths;
    }
  }
}
