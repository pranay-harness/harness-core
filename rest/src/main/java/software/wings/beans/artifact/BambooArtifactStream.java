package software.wings.beans.artifact;

import static java.util.stream.Collectors.toSet;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.EmbeddedUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.validation.Valid;

/**
 * Created by anubhaw on 11/22/16.
 */
@JsonTypeName("BAMBOO")
public class BambooArtifactStream extends ArtifactStream {
  @NotEmpty private String bambooSettingId;

  @NotEmpty private String jobname;

  @NotEmpty @Valid private List<ArtifactPathServiceEntry> artifactPathServices = Lists.newArrayList();

  /**
   * Instantiates a new Bamboo artifact stream.
   */
  public BambooArtifactStream() {
    super(SourceType.BAMBOO);
    super.setSourceName(jobname);
  }

  @Override
  public Set<String> getServiceIds() {
    return artifactPathServices.stream()
        .flatMap(artifactPathServiceEntry -> artifactPathServiceEntry.getServiceIds().stream())
        .collect(toSet());
  }

  /**
   * Gets bamboo setting id.
   *
   * @return the bamboo setting id
   */
  public String getBambooSettingId() {
    return bambooSettingId;
  }

  /**
   * Sets bamboo setting id.
   *
   * @param bambooSettingId the bamboo setting id
   */
  public void setBambooSettingId(String bambooSettingId) {
    this.bambooSettingId = bambooSettingId;
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
   * Gets artifact path services.
   *
   * @return the artifact path services
   */
  public List<ArtifactPathServiceEntry> getArtifactPathServices() {
    return artifactPathServices;
  }

  /**
   * Sets artifact path services.
   *
   * @param artifactPathServices the artifact path services
   */
  public void setArtifactPathServices(List<ArtifactPathServiceEntry> artifactPathServices) {
    this.artifactPathServices = artifactPathServices;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(bambooSettingId, jobname, artifactPathServices);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final BambooArtifactStream other = (BambooArtifactStream) obj;
    return Objects.equals(this.bambooSettingId, other.bambooSettingId) && Objects.equals(this.jobname, other.jobname)
        && Objects.equals(this.artifactPathServices, other.artifactPathServices);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("bambooSettingId", bambooSettingId)
        .add("jobname", jobname)
        .add("artifactPathServices", artifactPathServices)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String bambooSettingId;
    private String jobname;
    private List<ArtifactPathServiceEntry> artifactPathServices = Lists.newArrayList();
    private String sourceName;
    private boolean autoDownload = false;
    private boolean autoApproveForProduction = false;
    private List<ArtifactStreamAction> streamActions = new ArrayList<>();
    private Artifact lastArtifact;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

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
     * With bamboo setting id builder.
     *
     * @param bambooSettingId the bamboo setting id
     * @return the builder
     */
    public Builder withBambooSettingId(String bambooSettingId) {
      this.bambooSettingId = bambooSettingId;
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aBambooArtifactStream()
          .withBambooSettingId(bambooSettingId)
          .withJobname(jobname)
          .withArtifactPathServices(artifactPathServices)
          .withSourceName(sourceName)
          .withAutoDownload(autoDownload)
          .withAutoApproveForProduction(autoApproveForProduction)
          .withStreamActions(streamActions)
          .withLastArtifact(lastArtifact)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build bamboo artifact stream.
     *
     * @return the bamboo artifact stream
     */
    public BambooArtifactStream build() {
      BambooArtifactStream bambooArtifactStream = new BambooArtifactStream();
      bambooArtifactStream.setBambooSettingId(bambooSettingId);
      bambooArtifactStream.setJobname(jobname);
      bambooArtifactStream.setArtifactPathServices(artifactPathServices);
      bambooArtifactStream.setSourceName(sourceName);
      bambooArtifactStream.setAutoDownload(autoDownload);
      bambooArtifactStream.setAutoApproveForProduction(autoApproveForProduction);
      bambooArtifactStream.setStreamActions(streamActions);
      bambooArtifactStream.setLastArtifact(lastArtifact);
      bambooArtifactStream.setUuid(uuid);
      bambooArtifactStream.setAppId(appId);
      bambooArtifactStream.setCreatedBy(createdBy);
      bambooArtifactStream.setCreatedAt(createdAt);
      bambooArtifactStream.setLastUpdatedBy(lastUpdatedBy);
      bambooArtifactStream.setLastUpdatedAt(lastUpdatedAt);
      return bambooArtifactStream;
    }
  }
}
