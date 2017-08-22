package software.wings.beans.artifact;

import static software.wings.beans.artifact.AmazonS3ArtifactStream.Builder.anAmazonS3ArtifactStream;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.artifact.ArtifactoryArtifactStream.Builder;
import software.wings.stencils.UIOrder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by rktummala on 7/27/17.
 */
@JsonTypeName("AMAZON_S3")
public class AmazonS3ArtifactStream extends ArtifactStream {
  @UIOrder(4) @NotEmpty @Attributes(title = "Bucket", required = true) private String jobname;

  @UIOrder(6)
  @Attributes(title = "Meta-data Only (Artifact download not required)")
  public boolean getMetadataOnly() {
    return super.isMetadataOnly();
  }

  @UIOrder(6) @NotEmpty @Attributes(title = "Artifact Path", required = true) private List<String> artifactPaths;

  public AmazonS3ArtifactStream() {
    super(ArtifactStreamType.AMAZON_S3.name());
  }

  @SchemaIgnore
  @Override
  public String getArtifactDisplayName(String buildNo) {
    if (StringUtils.isBlank(getSourceName())) {
      return String.format("%s_%s_%s", getSourceName(), buildNo, getDateFormat().format(new Date()));
    }
    return String.format("%s_%s_%s", getJobname(), buildNo, getDateFormat().format(new Date()));
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

  @SchemaIgnore
  @Override
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withJobName(jobname)
        .withArtifactName(artifactPaths == null ? "" : artifactPaths.get(0))
        .build();
  }

  @Override
  public ArtifactStream clone() {
    return AmazonS3ArtifactStream.Builder.anAmazonS3ArtifactStream()
        .withAppId(getAppId())
        .withSourceName(getSourceName())
        .withSettingId(getSettingId())
        .withAutoApproveForProduction(getAutoApproveForProduction())
        .withJobname(getJobname())
        .withArtifactPaths(getArtifactPaths())
        .withMetadataOnly(getMetadataOnly())
        .build();
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

    private Builder() {}

    /**
     * A jenkins artifact stream builder.
     *
     * @return the builder
     */
    public static AmazonS3ArtifactStream.Builder anAmazonS3ArtifactStream() {
      return new AmazonS3ArtifactStream.Builder();
    }

    /**
     * With jobname builder.
     *
     * @param jobname the jobname
     * @return the builder
     */
    public AmazonS3ArtifactStream.Builder withJobname(String jobname) {
      this.jobname = jobname;
      return this;
    }

    /**
     * With artifact paths builder.
     *
     * @param artifactPaths the artifact paths
     * @return the builder
     */
    public AmazonS3ArtifactStream.Builder withArtifactPaths(List<String> artifactPaths) {
      this.artifactPaths = artifactPaths;
      return this;
    }

    /**
     * With source name builder.
     *
     * @param sourceName the source name
     * @return the builder
     */
    public AmazonS3ArtifactStream.Builder withSourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    /**
     * With setting id builder.
     *
     * @param settingId the setting id
     * @return the builder
     */
    public AmazonS3ArtifactStream.Builder withSettingId(String settingId) {
      this.settingId = settingId;
      return this;
    }

    /**
     * With service id builder.
     *
     * @param serviceId the service id
     * @return the builder
     */
    public AmazonS3ArtifactStream.Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public AmazonS3ArtifactStream.Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public AmazonS3ArtifactStream.Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public AmazonS3ArtifactStream.Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public AmazonS3ArtifactStream.Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public AmazonS3ArtifactStream.Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public AmazonS3ArtifactStream.Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With auto approve for production builder.
     *
     * @param autoApproveForProduction the auto approve for production
     * @return the builder
     */
    public AmazonS3ArtifactStream.Builder withAutoApproveForProduction(boolean autoApproveForProduction) {
      this.autoApproveForProduction = autoApproveForProduction;
      return this;
    }

    /**
     * With MetadataOnly
     */
    public AmazonS3ArtifactStream.Builder withMetadataOnly(boolean metadataOnly) {
      this.metadataOnly = metadataOnly;
      return this;
    }

    /**
     * With stream actions builder.
     *
     * @param streamActions the stream actions
     * @return the builder
     */
    public AmazonS3ArtifactStream.Builder withStreamActions(List<ArtifactStreamAction> streamActions) {
      this.streamActions = streamActions;
      return this;
    }

    /**
     * But builder.
     * @return the builder
     */
    public AmazonS3ArtifactStream.Builder but() {
      return anAmazonS3ArtifactStream()
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
          .withMetadataOnly(metadataOnly);
    }

    /**
     * Artifactory Artifact Stream
     */
    public AmazonS3ArtifactStream build() {
      AmazonS3ArtifactStream amazonS3ArtifactStream = new AmazonS3ArtifactStream();
      amazonS3ArtifactStream.setJobname(jobname);
      amazonS3ArtifactStream.setArtifactPaths(artifactPaths);
      amazonS3ArtifactStream.setSourceName(sourceName);
      amazonS3ArtifactStream.setSettingId(settingId);
      amazonS3ArtifactStream.setServiceId(serviceId);
      amazonS3ArtifactStream.setUuid(uuid);
      amazonS3ArtifactStream.setAppId(appId);
      amazonS3ArtifactStream.setCreatedBy(createdBy);
      amazonS3ArtifactStream.setCreatedAt(createdAt);
      amazonS3ArtifactStream.setLastUpdatedBy(lastUpdatedBy);
      amazonS3ArtifactStream.setLastUpdatedAt(lastUpdatedAt);
      amazonS3ArtifactStream.setAutoApproveForProduction(autoApproveForProduction);
      amazonS3ArtifactStream.setStreamActions(streamActions);
      amazonS3ArtifactStream.setMetadataOnly(metadataOnly);
      return amazonS3ArtifactStream;
    }
  }
}
