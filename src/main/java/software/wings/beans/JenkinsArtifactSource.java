package software.wings.beans;

import static java.util.stream.Collectors.toSet;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;

// TODO: Auto-generated Javadoc

/**
 * The Class JenkinsArtifactSource.
 */
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

  public String getJenkinsSettingId() {
    return jenkinsSettingId;
  }

  public void setJenkinsSettingId(String jenkinsSettingId) {
    this.jenkinsSettingId = jenkinsSettingId;
  }

  public String getJobname() {
    return jobname;
  }

  public void setJobname(String jobname) {
    this.jobname = jobname;
  }

  public List<ArtifactPathServiceEntry> getArtifactPathServices() {
    return artifactPathServices;
  }

  public void setArtifactPathServices(List<ArtifactPathServiceEntry> artifactPathServices) {
    this.artifactPathServices = artifactPathServices;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /* (non-Javadoc)
   * @see software.wings.beans.ArtifactSource#equals(java.lang.Object)
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
   * @see software.wings.beans.ArtifactSource#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), jenkinsSettingId, jobname, artifactPathServices);
  }

  /* (non-Javadoc)
   * @see software.wings.beans.ArtifactSource#toString()
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
   * The Class Builder.
   */
  public static final class Builder {
    private String jenkinsSettingId;
    private String name;
    private String jobname;
    private List<ArtifactPathServiceEntry> artifactPathServices = Lists.newArrayList();
    private String sourceName;
    private ArtifactType artifactType;

    private Builder() {}

    /**
     * A jenkins artifact source.
     *
     * @return the builder
     */
    public static Builder aJenkinsArtifactSource() {
      return new Builder();
    }

    /**
     * With jenkins setting id.
     *
     * @param jenkinsSettingId the jenkins setting id
     * @return the builder
     */
    public Builder withJenkinsSettingId(String jenkinsSettingId) {
      this.jenkinsSettingId = jenkinsSettingId;
      return this;
    }

    /**
     * With name.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With jobname.
     *
     * @param jobname the jobname
     * @return the builder
     */
    public Builder withJobname(String jobname) {
      this.jobname = jobname;
      return this;
    }

    /**
     * With artifact path services.
     *
     * @param artifactPathServices the artifact path services
     * @return the builder
     */
    public Builder withArtifactPathServices(List<ArtifactPathServiceEntry> artifactPathServices) {
      this.artifactPathServices = artifactPathServices;
      return this;
    }

    /**
     * With source name.
     *
     * @param sourceName the source name
     * @return the builder
     */
    public Builder withSourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    /**
     * With artifact type.
     *
     * @param artifactType the artifact type
     * @return the builder
     */
    public Builder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    /**
     * But.
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
          .withArtifactType(artifactType);
    }

    /**
     * Builds the.
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
      jenkinsArtifactSource.setArtifactType(artifactType);
      return jenkinsArtifactSource;
    }
  }
}
