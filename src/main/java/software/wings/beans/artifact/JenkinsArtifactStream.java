package software.wings.beans.artifact;

import static java.util.stream.Collectors.toSet;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.EmbeddedUser;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;

/**
 * The Class JenkinsArtifactStream.
 */
@JsonTypeName("JENKINS")
public class JenkinsArtifactStream extends ArtifactStream {
  @NotEmpty private String jenkinsSettingId;

  @NotEmpty private String jobname;

  @NotEmpty @Valid private List<ArtifactPathServiceEntry> artifactPathServices = Lists.newArrayList();

  /**
   * Instantiates a new jenkins artifact source.
   */
  public JenkinsArtifactStream() {
    super(SourceType.JENKINS);
    super.setSourceName(jobname);
  }

  @Override
  public Set<String> getServiceIds() {
    return artifactPathServices.stream()
        .flatMap(artifactPathServiceEntry -> artifactPathServiceEntry.getServiceIds().stream())
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

  /* (non-Javadoc)
   * @see software.wings.beans.artifact.ArtifactStream#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    JenkinsArtifactStream that = (JenkinsArtifactStream) o;
    return Objects.equal(jenkinsSettingId, that.jenkinsSettingId) && Objects.equal(jobname, that.jobname)
        && Objects.equal(artifactPathServices, that.artifactPathServices);
  }

  /* (non-Javadoc)
   * @see software.wings.beans.artifact.ArtifactStream#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), jenkinsSettingId, jobname, artifactPathServices);
  }

  /* (non-Javadoc)
   * @see software.wings.beans.artifact.ArtifactStream#toString()
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
    private String jobname;
    private List<ArtifactPathServiceEntry> artifactPathServices = Lists.newArrayList();
    private String sourceName;
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

    public static Builder aJenkinsArtifactStream() {
      return new Builder();
    }

    public Builder withJenkinsSettingId(String jenkinsSettingId) {
      this.jenkinsSettingId = jenkinsSettingId;
      return this;
    }

    public Builder withJobname(String jobname) {
      this.jobname = jobname;
      return this;
    }

    public Builder withArtifactPathServices(List<ArtifactPathServiceEntry> artifactPathServices) {
      this.artifactPathServices = artifactPathServices;
      return this;
    }

    public Builder withSourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    public Builder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    public Builder withDownloadType(ArtifactDownloadType downloadType) {
      this.downloadType = downloadType;
      return this;
    }

    public Builder withAutoApproveForProduction(boolean autoApproveForProduction) {
      this.autoApproveForProduction = autoApproveForProduction;
      return this;
    }

    public Builder withPostDownloadActions(List<PostArtifactDownloadAction> postDownloadActions) {
      this.postDownloadActions = postDownloadActions;
      return this;
    }

    public Builder withLastArtifact(Artifact lastArtifact) {
      this.lastArtifact = lastArtifact;
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

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
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

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder but() {
      return aJenkinsArtifactStream()
          .withJenkinsSettingId(jenkinsSettingId)
          .withJobname(jobname)
          .withArtifactPathServices(artifactPathServices)
          .withSourceName(sourceName)
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

    public JenkinsArtifactStream build() {
      JenkinsArtifactStream jenkinsArtifactStream = new JenkinsArtifactStream();
      jenkinsArtifactStream.setJenkinsSettingId(jenkinsSettingId);
      jenkinsArtifactStream.setJobname(jobname);
      jenkinsArtifactStream.setArtifactPathServices(artifactPathServices);
      jenkinsArtifactStream.setSourceName(sourceName);
      jenkinsArtifactStream.setArtifactType(artifactType);
      jenkinsArtifactStream.setDownloadType(downloadType);
      jenkinsArtifactStream.setAutoApproveForProduction(autoApproveForProduction);
      jenkinsArtifactStream.setPostDownloadActions(postDownloadActions);
      jenkinsArtifactStream.setLastArtifact(lastArtifact);
      jenkinsArtifactStream.setUuid(uuid);
      jenkinsArtifactStream.setAppId(appId);
      jenkinsArtifactStream.setCreatedBy(createdBy);
      jenkinsArtifactStream.setCreatedAt(createdAt);
      jenkinsArtifactStream.setLastUpdatedBy(lastUpdatedBy);
      jenkinsArtifactStream.setLastUpdatedAt(lastUpdatedAt);
      return jenkinsArtifactStream;
    }
  }
}
