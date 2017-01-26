package software.wings.beans.artifact;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.EmbeddedUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The Class JenkinsArtifactStream.
 */
@JsonTypeName("JENKINS")
public class JenkinsArtifactStream extends ArtifactStream {
  @NotEmpty @Attributes(title = "Job Name*") private String jobname;

  /**
   * Instantiates a new jenkins artifact source.
   */
  public JenkinsArtifactStream() {
    super(ArtifactStreamType.JENKINS.name());
  }

  /**
   * Gets artifact display name.
   *
   * @param buildNo the build no
   * @return the artifact display name
   */
  @Override
  @SchemaIgnore
  public String getArtifactDisplayName(String buildNo) {
    return String.format("%s_%s_%s", getJobname(), buildNo, getDateFormat().format(new Date()));
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
  public String getArtifactStreamType() {
    return super.getArtifactStreamType();
  }

  @Attributes(title = "Source Server")
  public String getSettingId() {
    return super.getSettingId();
  }

  @Attributes(title = "Artifact Path*")
  public List<ArtifactPathServiceEntry> getArtifactPathServices() {
    return super.getArtifactPathServices();
  }

  @Attributes(title = "Automatic Download")
  public boolean isAutoDownload() {
    return super.isAutoDownload();
  }

  @Attributes(title = "Auto-approved for Production")
  public boolean isAutoApproveForProduction() {
    return super.isAutoApproveForProduction();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String jobname;
    private String sourceName;
    private String settingId;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private List<ArtifactPathServiceEntry> artifactPathServices = Lists.newArrayList();
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean autoDownload = false;
    private boolean autoApproveForProduction = false;
    private List<ArtifactStreamAction> streamActions = new ArrayList<>();

    private Builder() {}

    /**
     * A jenkins artifact stream builder.
     *
     * @return the builder
     */
    public static Builder aJenkinsArtifactStream() {
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
     * With artifact path services builder.
     *
     * @param artifactPathServices the artifact path services
     * @return the builder
     */
    public Builder withArtifactPathServices(List<ArtifactPathServiceEntry> artifactPathServices) {
      this.artifactPathServices = artifactPathServices;
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
      return aJenkinsArtifactStream()
          .withJobname(jobname)
          .withSourceName(sourceName)
          .withSettingId(settingId)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withArtifactPathServices(artifactPathServices)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withAutoDownload(autoDownload)
          .withAutoApproveForProduction(autoApproveForProduction)
          .withStreamActions(streamActions);
    }

    /**
     * Build jenkins artifact stream.
     *
     * @return the jenkins artifact stream
     */
    public JenkinsArtifactStream build() {
      JenkinsArtifactStream jenkinsArtifactStream = new JenkinsArtifactStream();
      jenkinsArtifactStream.setJobname(jobname);
      jenkinsArtifactStream.setSourceName(sourceName);
      jenkinsArtifactStream.setSettingId(settingId);
      jenkinsArtifactStream.setUuid(uuid);
      jenkinsArtifactStream.setAppId(appId);
      jenkinsArtifactStream.setCreatedBy(createdBy);
      jenkinsArtifactStream.setCreatedAt(createdAt);
      jenkinsArtifactStream.setArtifactPathServices(artifactPathServices);
      jenkinsArtifactStream.setLastUpdatedBy(lastUpdatedBy);
      jenkinsArtifactStream.setLastUpdatedAt(lastUpdatedAt);
      jenkinsArtifactStream.setAutoDownload(autoDownload);
      jenkinsArtifactStream.setAutoApproveForProduction(autoApproveForProduction);
      jenkinsArtifactStream.setStreamActions(streamActions);
      return jenkinsArtifactStream;
    }
  }
}
