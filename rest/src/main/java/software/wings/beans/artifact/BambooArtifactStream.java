package software.wings.beans.artifact;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.EmbeddedUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by anubhaw on 11/22/16.
 */
@JsonTypeName("BAMBOO")
public class BambooArtifactStream extends ArtifactStream {
  @NotEmpty private String jobname;

  /**
   * Instantiates a new Bamboo artifact stream.
   */
  public BambooArtifactStream() {
    super(ArtifactStreamType.BAMBOO);
  }

  @Override
  public String getArtifactDisplayName(int buildNo) {
    return String.format("%s_%s_%s", getSourceName(), buildNo, dateFormat.format(new Date()));
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

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String sourceName;
    private String settingId;
    private String jobname;
    private List<ArtifactPathServiceEntry> artifactPathServices = Lists.newArrayList();
    private boolean autoDownload = false;
    private String uuid;
    private boolean autoApproveForProduction = false;
    private String appId;
    private EmbeddedUser createdBy;
    private List<ArtifactStreamAction> streamActions = new ArrayList<>();
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private Artifact lastArtifact;

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
     * With last artifact builder.
     *
     * @param lastArtifact the last artifact
     * @return the builder
     */
    public Builder withLastArtifact(Artifact lastArtifact) {
      this.lastArtifact = lastArtifact;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aBambooArtifactStream()
          .withSourceName(sourceName)
          .withSettingId(settingId)
          .withJobname(jobname)
          .withArtifactPathServices(artifactPathServices)
          .withAutoDownload(autoDownload)
          .withUuid(uuid)
          .withAutoApproveForProduction(autoApproveForProduction)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withStreamActions(streamActions)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withLastArtifact(lastArtifact);
    }

    /**
     * Build bamboo artifact stream.
     *
     * @return the bamboo artifact stream
     */
    public BambooArtifactStream build() {
      BambooArtifactStream bambooArtifactStream = new BambooArtifactStream();
      bambooArtifactStream.setSourceName(sourceName);
      bambooArtifactStream.setSettingId(settingId);
      bambooArtifactStream.setJobname(jobname);
      bambooArtifactStream.setArtifactPathServices(artifactPathServices);
      bambooArtifactStream.setAutoDownload(autoDownload);
      bambooArtifactStream.setUuid(uuid);
      bambooArtifactStream.setAutoApproveForProduction(autoApproveForProduction);
      bambooArtifactStream.setAppId(appId);
      bambooArtifactStream.setCreatedBy(createdBy);
      bambooArtifactStream.setStreamActions(streamActions);
      bambooArtifactStream.setCreatedAt(createdAt);
      bambooArtifactStream.setLastUpdatedBy(lastUpdatedBy);
      bambooArtifactStream.setLastUpdatedAt(lastUpdatedAt);
      bambooArtifactStream.setLastArtifact(lastArtifact);
      return bambooArtifactStream;
    }
  }
}
