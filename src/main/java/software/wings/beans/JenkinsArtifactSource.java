package software.wings.beans;

import static java.util.stream.Collectors.toSet;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;

public class JenkinsArtifactSource extends ArtifactSource {
  @NotEmpty private String jenkinsSettingId;

  private String name;

  @NotEmpty private String jobname;

  @NotEmpty @Valid private List<ArtifactPathServiceEntry> artifactPathServices = Lists.newArrayList();

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

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), jenkinsSettingId, jobname, artifactPathServices);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("artifactPathServices", artifactPathServices)
        .add("jenkinsSettingId", jenkinsSettingId)
        .add("jobname", jobname)
        .toString();
  }

  public static final class Builder {
    private String jenkinsSettingId;
    private String name;
    private String jobname;
    private List<ArtifactPathServiceEntry> artifactPathServices = Lists.newArrayList();
    private String sourceName;
    private ArtifactType artifactType;

    private Builder() {}

    public static Builder aJenkinsArtifactSource() {
      return new Builder();
    }

    public Builder withJenkinsSettingId(String jenkinsSettingId) {
      this.jenkinsSettingId = jenkinsSettingId;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
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

    public Builder but() {
      return aJenkinsArtifactSource()
          .withJenkinsSettingId(jenkinsSettingId)
          .withName(name)
          .withJobname(jobname)
          .withArtifactPathServices(artifactPathServices)
          .withSourceName(sourceName)
          .withArtifactType(artifactType);
    }

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
