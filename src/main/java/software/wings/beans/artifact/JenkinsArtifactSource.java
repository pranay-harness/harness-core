package software.wings.beans.artifact;

import static java.util.stream.Collectors.toSet;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.Service;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;

/**
 * The Class JenkinsArtifactSource.
 */
@JsonTypeName("JENKINS")
public class JenkinsArtifactSource extends ArtifactSource {
  @NotEmpty private String jenkinsSettingId;

  private String name;

  @NotEmpty private String jobname;

  @NotEmpty @Valid private List<ArtifactPathServiceEntry> artifactPathServices = Lists.newArrayList();

  /**
   * Instantiates a new jenkins artifact source.
   */
  public JenkinsArtifactSource() {
    super(SourceType.JENKINS);
  }

  @Override
  public Set<Service> getServices() {
    return artifactPathServices.stream()
        .flatMap(artifactPathServiceEntry -> artifactPathServiceEntry.getServices().stream())
        .collect(toSet());
  }

  /**
   * Gets jenkins setting id.
   *
   * @return the jenkins setting id
   */
  public String getJenkinsSettingId() {
    return jenkinsSettingId;
  }

  /**
   * Sets jenkins setting id.
   *
   * @param jenkinsSettingId the jenkins setting id
   */
  public void setJenkinsSettingId(String jenkinsSettingId) {
    this.jenkinsSettingId = jenkinsSettingId;
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

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /* (non-Javadoc)
   * @see software.wings.beans.artifact.ArtifactSource#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    JenkinsArtifactSource that = (JenkinsArtifactSource) o;
    return Objects.equal(jenkinsSettingId, that.jenkinsSettingId) && Objects.equal(jobname, that.jobname)
        && Objects.equal(artifactPathServices, that.artifactPathServices);
  }

  /* (non-Javadoc)
   * @see software.wings.beans.artifact.ArtifactSource#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), jenkinsSettingId, jobname, artifactPathServices);
  }

  /* (non-Javadoc)
   * @see software.wings.beans.artifact.ArtifactSource#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("artifactPathServices", artifactPathServices)
        .add("jenkinsSettingId", jenkinsSettingId)
        .add("jobname", jobname)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String jenkinsSettingId;
    private String name;
    private String jobname;
    private List<ArtifactPathServiceEntry> artifactPathServices = Lists.newArrayList();
    private String sourceName;
    private SourceType sourceType;
    private ArtifactType artifactType;
    private ArtifactDownloadType downloadType;
    private boolean autoApproveForProduction = false;
    private List<PostArtifactDownloadAction> postDownloadActions;
    private Artifact lastArtifact;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * A jenkins artifact source builder.
     *
     * @return the builder
     */
    public static Builder aJenkinsArtifactSource() {
      return new Builder();
    }

    /**
     * With jenkins setting id builder.
     *
     * @param jenkinsSettingId the jenkins setting id
     * @return the builder
     */
    public Builder withJenkinsSettingId(String jenkinsSettingId) {
      this.jenkinsSettingId = jenkinsSettingId;
      return this;
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
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
     * With source type builder.
     *
     * @param sourceType the source type
     * @return the builder
     */
    public Builder withSourceType(SourceType sourceType) {
      this.sourceType = sourceType;
      return this;
    }

    /**
     * With artifact type builder.
     *
     * @param artifactType the artifact type
     * @return the builder
     */
    public Builder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    /**
     * With download type builder.
     *
     * @param downloadType the download type
     * @return the builder
     */
    public Builder withDownloadType(ArtifactDownloadType downloadType) {
      this.downloadType = downloadType;
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
     * With post download actions builder.
     *
     * @param postDownloadActions the post download actions
     * @return the builder
     */
    public Builder withPostDownloadActions(List<PostArtifactDownloadAction> postDownloadActions) {
      this.postDownloadActions = postDownloadActions;
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
      return aJenkinsArtifactSource()
          .withJenkinsSettingId(jenkinsSettingId)
          .withName(name)
          .withJobname(jobname)
          .withArtifactPathServices(artifactPathServices)
          .withSourceName(sourceName)
          .withSourceType(sourceType)
          .withArtifactType(artifactType)
          .withDownloadType(downloadType)
          .withAutoApproveForProduction(autoApproveForProduction)
          .withPostDownloadActions(postDownloadActions)
          .withLastArtifact(lastArtifact)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build jenkins artifact source.
     *
     * @return the jenkins artifact source
     */
    public JenkinsArtifactSource build() {
      JenkinsArtifactSource jenkinsArtifactSource = new JenkinsArtifactSource();
      jenkinsArtifactSource.setJenkinsSettingId(jenkinsSettingId);
      jenkinsArtifactSource.setName(name);
      jenkinsArtifactSource.setJobname(jobname);
      jenkinsArtifactSource.setArtifactPathServices(artifactPathServices);
      jenkinsArtifactSource.setSourceName(sourceName);
      jenkinsArtifactSource.setSourceType(sourceType);
      jenkinsArtifactSource.setArtifactType(artifactType);
      jenkinsArtifactSource.setDownloadType(downloadType);
      jenkinsArtifactSource.setAutoApproveForProduction(autoApproveForProduction);
      jenkinsArtifactSource.setPostDownloadActions(postDownloadActions);
      jenkinsArtifactSource.setLastArtifact(lastArtifact);
      jenkinsArtifactSource.setUuid(uuid);
      jenkinsArtifactSource.setAppId(appId);
      jenkinsArtifactSource.setCreatedBy(createdBy);
      jenkinsArtifactSource.setCreatedAt(createdAt);
      jenkinsArtifactSource.setLastUpdatedBy(lastUpdatedBy);
      jenkinsArtifactSource.setLastUpdatedAt(lastUpdatedAt);
      return jenkinsArtifactSource;
    }
  }
}
